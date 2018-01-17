package io.eremon

import scala.concurrent.ExecutionContext

import org.scalatest.time.{ Second, Seconds, Span }

import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.impl.spotify.DockerKitSpotify

import test._
import criteria._

/**
 * Verifies the Repository behaves as expected
 */
class RepositorySpec extends Spec
  with DockerTestKit
  with DockerKitSpotify
  with DockerMongoDBTestKit {
  import RepositorySpec._

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  lazy implicit val database = makeConnector

  lazy val testRepository = new TestRepository(database)

  before {
    clean(database)
  }

  override def afterAll(): Unit = {
    close(database)
    super.afterAll()

  }

  val id = ID.generate()
  val entity = Test("Linus Torvalds", 47, Set("Linux"), id)

  "mongodb node" should "be ready with log line checker" in {
    isContainerReady(mongodbContainer).futureValue shouldBe true
    mongodbContainer.getPorts().futureValue.get(27017) should not be empty
    mongodbContainer.getIpAddresses().futureValue should not be Seq.empty
  }

  "A ReactiveRepository" should "support insertion and find by id" in {
    testRepository.insert(entity).futureValue shouldBe entity
    testRepository.findById(id).futureValue shouldBe Some(entity)
  }

  it should "support bulk insert" in {
    val otherEntity = Test("Richard Stallman ", 64, Set("GNU"), ID.generate())
    testRepository.bulkInsert(List(entity, otherEntity)).map(_.n).futureValue shouldBe 2
    testRepository.count.futureValue shouldBe 2
    testRepository.findAll().futureValue shouldBe List(entity, otherEntity)
  }

  it should "support deletion" in {
    testRepository.insert(entity).futureValue shouldBe entity
    testRepository.removeById(id).futureValue shouldBe OperationSuccess
    testRepository.findById(id).futureValue shouldBe None
  }

  it should "support find by a field" in {
    import Typed._
    testRepository.insert(entity).futureValue shouldBe entity
    testRepository.findOne(criteria[Test](_.name) === "Linus Torvalds").futureValue shouldBe Some(entity)
  }

  it should "support update an entity" in {
    import Typed._
    testRepository.insert(entity).futureValue shouldBe entity
    testRepository.count.futureValue shouldBe 1
    testRepository.updateById(id, entity.copy(age = 46)).futureValue shouldBe Some(entity.copy(age = 46))
    testRepository.count.futureValue shouldBe 1
    testRepository.updateBy(criteria[Test](_.name) === "Linus Torvalds", entity).futureValue shouldBe Some(entity)
    testRepository.count.futureValue shouldBe 1
  }

  it should "support $push to an entity" in {
    testRepository.insert(entity).futureValue shouldBe entity
    testRepository.count.futureValue shouldBe 1
    testRepository.updateById(id, $push("softwares", "Git")).futureValue shouldBe Some(entity.copy(softwares = Set("Linux", "Git")))
    testRepository.count.futureValue shouldBe 1
    testRepository.findById(id).futureValue shouldBe Some(entity.copy(softwares = Set("Linux", "Git")))
  }

  it should "support $pull to an entity" in {
    testRepository.insert(entity).futureValue shouldBe entity
    testRepository.count.futureValue shouldBe 1
    testRepository.updateById(id, $pull("softwares", "Linux")).futureValue shouldBe Some(entity.copy(softwares = Set.empty))
    testRepository.count.futureValue shouldBe 1
    testRepository.findById(id).futureValue shouldBe Some(entity.copy(softwares = Set.empty))
  }

  it should "support multi update operation to an entity" in {
    testRepository.insert(entity).futureValue shouldBe entity
    testRepository.count.futureValue shouldBe 1
    testRepository.updateById(id, $set("name", "Linus"), $set("age", 48)).futureValue shouldBe Some(entity.copy(name = "Linus", age = 48))
    testRepository.count.futureValue shouldBe 1
    testRepository.findById(id).futureValue shouldBe Some(entity.copy(name = "Linus", age = 48))
  }
}

object RepositorySpec {
  import reactivemongo.bson._
  import reactivemongo.bson.Macros.Annotations._

  case class Test(name: String, age: Int, softwares: Set[String], @Key("_id") id: ID = ID.generate())

  implicit val testReader: BSONDocumentReader[Test] = Macros.reader[Test]
  implicit val testWriter: BSONDocumentWriter[Test] = Macros.writer[Test]

  class TestRepository(database: MongoDB)(implicit ec: ExecutionContext) extends ReactiveRepository[Test](
    database,
    "test",
    testReader,
    testWriter,
    ec) {

  }

}