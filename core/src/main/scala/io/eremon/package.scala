package io

import scala.concurrent._

import reactivemongo.api.{ DB, MongoConnection }
import reactivemongo.api.collections.bson.BSONCollection

package object eremon extends Connection {

  case class QueryOpt(offset: Int, limit: Int)

  case class MongoDB(name: String, connection: MongoConnection, instance: () => Future[DB]) {

    def collection(collName: String)(implicit ec: ExecutionContext): MongoCollection =
      MongoCollection(collName, this, () => instance().map(_.collection(collName)))

  }

  case class MongoCollection(name: String, database: MongoDB, instance: () => Future[BSONCollection])

}