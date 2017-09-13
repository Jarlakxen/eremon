package io.eremon

import scala.concurrent._
import scala.reflect._
import scala.util._

import org.slf4j.LoggerFactory
import reactivemongo.api.{ ReadPreference, QueryOpts, Cursor }
import reactivemongo.api.commands.{ WriteConcern, WriteResult, MultiBulkWriteResult }
import reactivemongo.api.indexes.Index
import reactivemongo.bson.{ BSONDocument, BSONDocumentReader, BSONDocumentWriter }
import reactivemongo.core.errors.GenericDatabaseException

abstract class ReactiveRepository[A <: Any](
  database: MongoDB,
  collectionName: String,
  entityReader: BSONDocumentReader[A],
  entityWriter: BSONDocumentWriter[A],
  executionContext: ExecutionContext)(implicit atag: ClassTag[A], idtag: ClassTag[ID]) {
  import ReactiveRepository._

  private implicit val ec = executionContext
  private implicit val entityReaderImplicit = entityReader
  private implicit val entityWriterImplicit = entityWriter

  private val logger = LoggerFactory.getLogger(this.getClass)

  val ensureIndexesRetryInterval = 10000

  val defaultReadPreference = database.connection.options.readPreference

  val collection: MongoCollection = database.collection(collectionName)

  def indexes: Seq[Index] = Seq.empty

  protected def $id(id: ID) = BSONDocument(Id -> id)

  def find(query: BSONDocument, opt: QueryOpt, sorting: BSONDocument): Future[List[A]] =
    find(query, Some(opt), sorting, None)

  def find(query: BSONDocument, readPreference: ReadPreference): Future[List[A]] =
    find(query, None, BSONDocument.empty, Some(readPreference))

  def find(query: BSONDocument, opt: QueryOpt, sorting: BSONDocument, readPreference: ReadPreference): Future[List[A]] =
    find(query, Some(opt), BSONDocument.empty, Some(readPreference))

  def find(query: BSONDocument, opt: Option[QueryOpt] = None, sorting: BSONDocument, readPreference: Option[ReadPreference]): Future[List[A]] = {
    val (skip, limit) = opt.map(opt => (opt.offset, opt.limit)).getOrElse((0, Int.MaxValue))
    val rp = readPreference.getOrElse(defaultReadPreference)

    for {
      instance <- collection.instance()
      result <- instance.find(query).options(QueryOpts(skip, limit)).sort(sorting).cursor[A](rp).collect[List](limit, Cursor.FailOnError[List[A]]())
    } yield result
  }

  def findAll(opt: QueryOpt, sorting: BSONDocument): Future[List[A]] =
    findAll(Some(opt), sorting, None)

  def findAll(opt: QueryOpt, sorting: BSONDocument, readPreference: ReadPreference): Future[List[A]] =
    findAll(Some(opt), sorting, Some(readPreference))

  def findAll(opt: Option[QueryOpt] = None, sorting: BSONDocument = BSONDocument.empty, readPreference: Option[ReadPreference] = None): Future[List[A]] =
    find(BSONDocument.empty, opt, sorting, readPreference)

  def findById(id: ID, readPreference: ReadPreference): Future[Option[A]] =
    findById(id, Some(readPreference))

  def findById(id: ID, readPreference: Option[ReadPreference] = None): Future[Option[A]] =
    findOne($id(id))

  def findOne(query: BSONDocument, readPreference: Option[ReadPreference] = None): Future[Option[A]] =
    for {
      instance <- collection.instance()
      result <- instance.find(query).one[A](readPreference.getOrElse(defaultReadPreference))
    } yield result

  def count: Future[Int] =
    for {
      instance <- collection.instance()
      result <- instance.count(None)
    } yield result

  def count(opt: QueryOpt): Future[Int] =
    count(None, Some(opt))

  def count(selector: BSONDocument, opt: QueryOpt): Future[Int] =
    count(Some(selector), Some(opt))

  def count(selector: BSONDocument, opt: Option[QueryOpt] = None): Future[Int] =
    count(Some(selector), opt)

  def count(selector: Option[BSONDocument], opt: Option[QueryOpt]): Future[Int] = {
    val (skip, limit) = opt.map(opt => (opt.offset, opt.limit)).getOrElse((0, Int.MaxValue))
    for {
      instance <- collection.instance()
      result <- instance.count(selector, limit, skip)
    } yield result
  }

  def removeAll(writeConcern: WriteConcern = WriteConcern.Default): Future[WriteResult] =
    remove(BSONDocument.empty, writeConcern)

  def removeById(id: ID, writeConcern: WriteConcern = WriteConcern.Default): Future[WriteResult] =
    remove($id(id), writeConcern)

  def remove(query: BSONDocument, writeConcern: WriteConcern = WriteConcern.Default): Future[WriteResult] =
    for {
      instance <- collection.instance()
      result <- instance.remove(query, writeConcern)
    } yield result

  def drop: Future[Boolean] =
    for {
      instance <- collection.instance()
      result <- instance.drop(false)
    } yield result

  def insert(entity: A, writeConcern: WriteConcern = WriteConcern.Default): Future[WriteResult] =
    for {
      instance <- collection.instance()
      result <- instance.insert(entity, writeConcern)
    } yield result

  def bulkInsert(entities: Traversable[A]): Future[MultiBulkWriteResult] =
    for {
      instance <- collection.instance()
      result <- instance.bulkInsert(entities.map(entityWriter.write(_)).toStream, false)
    } yield result

  private def ensureIndex(index: Index): Future[Boolean] =
    for {
      instance <- collection.instance()
      result <- instance.indexesManager.create(index).flatMap {
        case wr if !wr.ok => Future.failed(new GenericDatabaseException(wr.writeErrors.map(_.errmsg).mkString(", "), wr.code))
        case wr => Future.successful(wr.ok)
      }
    } yield result

  def ensureIndexes: Future[Boolean] = {
    for {
      instance <- collection.instance()
      pendingIdxs <- instance.indexesManager(ec).list().map(idxs => {
        val currentIdx = idxs.map(_.name).flatten
        val pendingIdxs = indexes.filter { newIdx => !currentIdx.contains(newIdx.name.get) }
        logger.debug(s"Current idxs: ${currentIdx.mkString(", ")}")
        logger.debug(s"Pending idxs: ${pendingIdxs.map(idx => idx.name.getOrElse(idx)).mkString(", ")}")
        pendingIdxs
      })
      insertedIdx <- Future.sequence {
        pendingIdxs.map(idx =>
          ensureIndex(idx).andThen {
            case Success(result) =>
              logger.debug(s"Index ${idx.name.getOrElse(idx)} inserted")
            case Failure(ex) =>
              logger.error(s"Index ${idx.name.getOrElse(idx)} fail", ex)
          })
      }
    } yield insertedIdx.forall(identity)
  }

}

object ReactiveRepository {
  protected val Id = "_id"
  protected val DuplicateKeyError = "E11000"

  class BulkInsertRejected extends Exception("No objects inserted. Error converting some or all to JSON")
}