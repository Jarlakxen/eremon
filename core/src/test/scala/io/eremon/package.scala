package io.eremon

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._

import org.slf4j._
import com.whisk.docker._

object test {

  /**
   * A definition to start a MongoDB isntance inside a Docker container to be used in tests.
   *
   * See https://github.com/whisklabs/docker-it-scala
   * See https://finelydistributed.io/integration-testing-with-docker-and-scala-85659d037740
   *
   * https://github.com/whisklabs/docker-it-scala/blob/master/config/src/test/resources/application.conf
   *
   */
  trait DockerMongoDBTestKit extends DockerKit {

    implicit val logger = LoggerFactory.getLogger(this.getClass)

    private val DefaultMongoDBPort = 27017

    val mongodbContainer = DockerContainer("mongo:3.4", Some("mongoDB"))
      .withPorts(DefaultMongoDBPort -> None)
      .withReadyChecker(LogLineContains("waiting for connections on port"))
      .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0", "--port", DefaultMongoDBPort.toString)

    logger.info(s"Configured MongoDB on port $DefaultMongoDBPort")
    abstract override def dockerContainers: List[DockerContainer] = mongodbContainer :: super.dockerContainers

    val futureMongoPort =
      for {
        _ <- isContainerReady(mongodbContainer) // Wait for the container to be ready
        ports <- mongodbContainer.getPorts()
      } yield ports(DefaultMongoDBPort)

    protected def makeConnector: MongoDB = Await.result(
      (for {
        port <- futureMongoPort
        db <- {
          val mongoUri = s"mongodb://localhost:$port/test-db"
          logger.info(s"MONGODB_URI=$mongoUri")
          Future.fromTry(MongoConnector(mongoUri))
        }
        _ <- db.instance()
      } yield db),
      10 seconds)

    protected def clean(db: MongoDB) = Await.ready(db.instance().flatMap(_.drop()), 10 seconds)

    protected def close(db: MongoDB) = db.connection.close()
  }

  case class LogLineContains(str: String)(implicit logger: Logger) extends DockerReadyChecker {

    private def trace(str: String) = {
      logger.debug(s"MongoDB: $str")
      str
    }

    override def apply(container: DockerContainerState)(implicit docker: DockerCommandExecutor, ec: ExecutionContext): Future[Boolean] = {
      for {
        id <- container.id
        _ <- Future.successful(logger.info(s"Start waiting on container $id"))
        _ <- docker.withLogStreamLinesRequirement(id, withErr = true)(trace(_).contains(str))
      } yield {
        true
      }
    }
  }

}