package io.eremon

import reactivemongo.bson._

package object criteria {
  /// Implicit Conversions
  implicit object ExpressionWriter extends BSONDocumentWriter[Expression] {
    override def write(expr: Expression): BSONDocument =
      toBSONDocument(expr);
  }

  implicit def toBSONDocument(expr: Expression): BSONDocument =
    expr match {
      case Expression(Some(name), BSONElement(field, element)) if (name == field) =>
        BSONDocument(field -> element);

      case Expression(Some(name), element) =>
        BSONDocument(name -> BSONDocument(element));

      case Expression(None, BSONElement("", _)) =>
        BSONDocument.empty;

      case Expression(None, element) =>
        BSONDocument(element);
    }

  implicit def toBSONElement(expr: Expression): BSONElement =
    expr.element;

  /**
   * The '''ValueBuilder'' type is a model of the ''type class'' pattern used to
   * produce a ''T''-specific [[reactivemongo.bson.BSONValue]] instance.
   *
   * @author svickers
   *
   */
  trait ValueBuilder[T] {
    def bson(v: T): BSONValue;
  }

  implicit def bsonValueIdentityValue[T <: BSONValue]: ValueBuilder[T] =
    new ValueBuilder[T] {
      override def bson(v: T): T = v;
    }

  implicit object DateTimeValue
      extends ValueBuilder[java.util.Date] {
    override def bson(v: java.util.Date): BSONValue = BSONDateTime(
      v.getTime
    );
  }

  implicit object BooleanValue
      extends ValueBuilder[Boolean] {
    override def bson(v: Boolean): BSONValue = BSONBoolean(v);
  }

  implicit object DoubleValue
      extends ValueBuilder[Double] {
    override def bson(v: Double): BSONValue = BSONDouble(v);
  }

  implicit object IntValue
      extends ValueBuilder[Int] {
    override def bson(v: Int): BSONValue = BSONInteger(v);
  }

  implicit object LongValue
      extends ValueBuilder[Long] {
    override def bson(v: Long): BSONValue = BSONLong(v);
  }

  implicit object StringValue
      extends ValueBuilder[String] {
    override def bson(v: String): BSONValue = BSONString(v);
  }

  implicit object SymbolValue
      extends ValueBuilder[Symbol] {
    override def bson(v: Symbol): BSONValue = BSONSymbol(v.name);
  }

  implicit object TimestampValue
      extends ValueBuilder[java.sql.Timestamp] {
    override def bson(v: java.sql.Timestamp): BSONValue = BSONTimestamp(
      v.getTime
    );
  }
}