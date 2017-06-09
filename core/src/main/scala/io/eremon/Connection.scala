package io.eremon

import scala.concurrent._
import scala.util._
import reactivemongo.api.{ FailoverStrategy, MongoDriver, MongoConnection }

import exceptions._

trait Connection {

  object MongoConnector {

    def apply(mongoUri: String, failoverStrategy: Option[FailoverStrategy])(implicit ec: ExecutionContext): Try[MongoDB] = {
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

}