package io.eremon

import reactivemongo.bson._

trait UpdateOperation {
  def toDocument: BSONDocument
}

case class $pull[T](arrayField: String, criteria: BSONValue) extends UpdateOperation {
  def toDocument: BSONDocument =
    BSONDocument("$pull" -> BSONDocument(arrayField -> criteria))
}

case class $push[T](arrayField: String, value: T)(implicit w: BSONWriter[T, _ <: BSONValue]) extends UpdateOperation {
  def toDocument: BSONDocument =
    BSONDocument("$push" -> BSONDocument(arrayField -> w.write(value)))
}

case class $set[T](field: String, value: T)(implicit w: BSONWriter[T, _ <: BSONValue]) extends UpdateOperation {
  def toDocument: BSONDocument =
    BSONDocument("$set" -> BSONDocument(field -> w.write(value)))
}

case class $unset[T](field: String) extends UpdateOperation {
  def toDocument: BSONDocument =
    BSONDocument("$unset" -> BSONDocument(field -> BSONString("")))
}