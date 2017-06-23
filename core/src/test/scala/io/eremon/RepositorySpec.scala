package io.eremon

import scala.concurrent._
import scala.concurrent.Await._
import scala.concurrent.duration._

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeEach
import org.specs2.specification.AfterAll

import test._

/**
 * Verifies the Repository behaves as expected
 */
@RunWith(classOf[JUnitRunner])
class RepositorySpec extends Specification with DockerMongoDBTestKit with BeforeEach with AfterAll {
  import RepositorySpec._
  sequential

  lazy implicit val database = makeConnector

  lazy val testRepository = new TestRepository(database)

  protected def before: Unit = {
    clean(database)
  }

  override def afterAll: Unit = {
    super.afterAll()
    close(database)
  }

  def sync[T](f: Future[T]): T =
    result(f, 10 seconds)

  "A reactive repository" should {
    "insert, find and delete" in {
      val id = ID.generate()

      sync(testRepository.insert(Test("Linus Torvalds", 47, id)).map(_.ok)) must beTrue
      sync(testRepository.findById(id)) must beSome(Test("Linus Torvalds", 47, id))
    }
  }
}

object RepositorySpec {
  import reactivemongo.bson._
  import reactivemongo.bson.Macros.Annotations._

  case class Test(name: String, age: Int, @Key("_id") id: ID = ID.generate())

  implicit val testReader: BSONDocumentReader[Test] = Macros.reader[Test]
  implicit val testWriter: BSONDocumentWriter[Test] = Macros.writer[Test]

  class TestRepository(database: MongoDB)(implicit ec: ExecutionContext) extends ReactiveRepository[Test](
    database,
    "test",
    testReader,
    testWriter,
    ec
  ) {

  }

}