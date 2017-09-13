package io

import scala.concurrent._
import scala.util._

import reactivemongo.api.{ DefaultDB, FailoverStrategy, MongoDriver, MongoConnection }
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONObjectID

import eremon.exceptions._

package object eremon {

  type ID = BSONObjectID

  object ID {
    val generate: () => ID = BSONObjectID.generate _

    def of(id: String): Try[ID] = BSONObjectID.parse(id).recoverWith { case _ => Failure(new RuntimeException(s"Invalid ID format $id")) }
  }

  case class QueryOpt(offset: Int, limit: Int)

  case class MongoDB(name: String, connection: MongoConnection, instance: () => Future[DefaultDB]) {

    def collection(collName: String)(implicit ec: ExecutionContext): MongoCollection =
      MongoCollection(collName, this, () => instance().map(_.collection(collName)))

  }

  case class MongoCollection(name: String, database: MongoDB, instance: () => Future[BSONCollection])

  object MongoConnector {

    def apply(mongoUri: String, failoverStrategy: Option[FailoverStrategy] = None)(implicit ec: ExecutionContext): Try[MongoDB] = {
      val driver = new MongoDriver()
      for {
        uri <- MongoConnection.parseURI(mongoUri)
        connection = driver.connection(uri)
        dbName <- uri.db.map(Success(_)).getOrElse(Failure(MissingDatabaseName(mongoUri)))
      } yield {
        val instance = failoverStrategy.map(fos => () => connection.database(dbName, fos)).getOrElse(() => connection.database(dbName))
        MongoDB(dbName, connection, instance)
      }
    }

  }

  case object OperationSuccess

}