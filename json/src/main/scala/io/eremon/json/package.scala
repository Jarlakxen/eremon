package io.eremon

import scala.util.{ Failure, Success }

import io.circe._

import _root_.reactivemongo.bson.{
  BSONArray,
  BSONBoolean,
  BSONDateTime,
  BSONDocument,
  BSONDocumentReader,
  BSONDocumentWriter,
  BSONDouble,
  BSONInteger,
  BSONLong,
  BSONNull,
  BSONObjectID,
  BSONString,
  BSONTimestamp,
  BSONUndefined,
  BSONValue,
  BSONWriter
}

package object json extends ImplicitBSONHandlers {

  @SuppressWarnings(Array("NullAssignment"))
  class JSONException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

}

object BSONFormats extends BSONFormats {

}

/**
 * JSON Formats for BSONValues.
 */
sealed trait BSONFormats extends LowerImplicitBSONHandlers {

  trait PartialDecoder[T <: BSONValue] extends Decoder[T] {
    val partial: Json => Option[Decoder.Result[T]]

    override def apply(cursor: HCursor): Decoder.Result[T] = partial(cursor.value).getOrElse(Left(DecodingFailure(s"${this.getClass.getSimpleName} cannot handle json value: ${cursor.value}", Nil)))
  }

  trait PartialEncoder[T <: BSONValue] extends Encoder[T] {
    def partial: PartialFunction[BSONValue, Json]

    override def apply(t: T): Json = partial(t)
  }

  implicit object BSONDoubleDecoder extends PartialDecoder[BSONDouble] {
    val partial: Json => Option[Decoder.Result[BSONDouble]] = json =>
      for {
        number <- json.asNumber
        double <- {
          number.toInt match {
            case Some(_) => None
            case None => Some(number.toDouble)
          }
        }
      } yield Right(BSONDouble(double))
  }

  implicit object BSONDoubleEncoder extends PartialEncoder[BSONDouble] {
    private val jsNaN = {
      @SuppressWarnings(Array("LooksLikeInterpolatedString"))
      @inline def json = Json.obj(f"$$double" -> Json.fromString("NaN"))
      json
    }

    val partial: PartialFunction[BSONValue, Json] = {
      case BSONDouble(v) if v.isNaN => jsNaN
      case BSONDouble(v) => Json.fromDouble(v).get
    }
  }

  implicit object BSONStringDecoder extends PartialDecoder[BSONString] {
    val partial: Json => Option[Decoder.Result[BSONString]] = _.asString.map(v => Right(BSONString(v)))
  }

  implicit object BSONStringEncoder extends PartialEncoder[BSONString] {
    val partial: PartialFunction[BSONValue, Json] = {
      case str: BSONString => Json.fromString(str.value)
    }
  }

  implicit object BSONObjectIDDecoder extends PartialDecoder[BSONObjectID] {
    val partial: Json => Option[Decoder.Result[BSONObjectID]] = json =>
      for {
        obj <- json.asObject
        field <- obj(f"$$oid")
        fieldStr <- field.asString
      } yield BSONObjectID.parse(fieldStr) match {
        case Success(id) => Right(id)
        case Failure(er) => Left(DecodingFailure(s"${this.getClass.getSimpleName} cannot handle json value $json: ${er.getMessage}", Nil))
      }
  }

  implicit object BSONObjectIDEncoder extends PartialEncoder[BSONObjectID] {
    val partial: PartialFunction[BSONValue, Json] = {
      case oid: BSONObjectID => Json.obj(f"$$oid" -> Json.fromString(oid.stringify))
    }
  }

  implicit object BSONBooleanDecoder extends PartialDecoder[BSONBoolean] {
    val partial: Json => Option[Decoder.Result[BSONBoolean]] = _.asBoolean.map(b => Right(BSONBoolean(b)))
  }

  implicit object BSONBooleanEncoder extends PartialEncoder[BSONBoolean] {
    val partial: PartialFunction[BSONValue, Json] = {
      case boolean: BSONBoolean => Json.fromBoolean(boolean.value)
    }
  }

  implicit object BSONNullDecoder extends PartialDecoder[BSONNull.type] {
    val partial: Json => Option[Decoder.Result[BSONNull.type]] = json => {
      if (json.isNull)
        Some(Right(BSONNull))
      else
        None
    }
  }

  implicit object BSONNullEncoder extends PartialEncoder[BSONNull.type] {
    val partial: PartialFunction[BSONValue, Json] = {
      case BSONNull => Json.Null
    }
  }

  implicit object BSONIntegerDecoder extends PartialDecoder[BSONInteger] {
    private object IntValue {
      @SuppressWarnings(Array("LooksLikeInterpolatedString"))
      def apply(json: Json): Option[Int] =
        for {
          obj <- json.asObject
          field <- obj(f"$$int")
          fieldNum <- field.asNumber
          value <- fieldNum.toInt
        } yield value
    }

    val partial: Json => Option[Decoder.Result[BSONInteger]] = json => {
      val int = for {
        field <- json.asNumber
        value <- field.toInt
      } yield value

      int.orElse(IntValue(json)).map(i => Right(BSONInteger(i)))
    }
  }

  implicit object BSONIntegerEncoder extends PartialEncoder[BSONInteger] {
    val partial: PartialFunction[BSONValue, Json] = {
      case BSONInteger(i) => Json.fromInt(i)
    }
  }

  implicit object BSONLongDecoder extends PartialDecoder[BSONLong] {
    private object LongValue {
      @SuppressWarnings(Array("LooksLikeInterpolatedString"))
      def apply(json: Json): Option[Long] =
        for {
          obj <- json.asObject
          field <- obj(f"$$long")
          fieldNum <- field.asNumber
          value <- fieldNum.toLong
        } yield value
    }

    val partial: Json => Option[Decoder.Result[BSONLong]] = json => {
      val long = for {
        field <- json.asNumber
        value <- field.toLong
      } yield value

      long.orElse(LongValue(json)).map(l => Right(BSONLong(l)))
    }
  }

  implicit object BSONLongEncoder extends PartialEncoder[BSONLong] {
    val partial: PartialFunction[BSONValue, Json] = {
      case BSONLong(l) => Json.fromLong(l)
    }
  }

  implicit object BSONUndefinedDecoder extends PartialDecoder[BSONUndefined.type] {
    val partial: Json => Option[Decoder.Result[BSONUndefined.type]] = json =>
      for {
        obj <- json.asObject
        field <- obj(f"$$undefined")
        value <- field.asBoolean
        result <- {
          if (value) {
            Some(Right(BSONUndefined))
          } else {
            None
          }
        }
      } yield result
  }

  implicit object BSONUndefinedEncoder extends PartialEncoder[BSONUndefined.type] {
    val partial: PartialFunction[BSONValue, Json] = {
      case BSONUndefined => Json.obj(f"$$undefined" -> Json.True)
    }
  }

  implicit object BSONDocumentDecoder extends PartialDecoder[BSONDocument] {

    private def bson(obj: JsonObject): Decoder.Result[BSONDocument] = {
      val init: Either[List[DecodingFailure], List[(String, BSONValue)]] = Right(Nil)
      val result = obj.toList
        .map({ case (fieldName, value) => (fieldName, toBSON(value)) })
        .foldLeft(init) {
          case (Right(acc), (field, Right(bson))) => Right(acc :+ (field -> bson))
          case (Right(acc), (field, Left(error))) => Left(List(error))
          case (Left(acc), (field, Right(bson))) => Left(acc)
          case (Left(acc), (field, Left(error))) => Left(acc :+ error)
        }
      (result: @unchecked) match {
        case Right(fieldsAndValues) => Right(BSONDocument(fieldsAndValues))
        case Left(error :: errors) => Left(error)
      }
    }

    val partial: Json => Option[Decoder.Result[BSONDocument]] = json =>
      json.asObject.map(bson(_))
  }

  implicit object BSONDocumentEncoder extends PartialEncoder[BSONDocument] {
    private def json(bson: BSONDocument): Json =
      Json.obj(bson.elements.map(elem => elem.name -> toJSON(elem.value)).toSeq: _*)

    val partial: PartialFunction[BSONValue, Json] = {
      case doc: BSONDocument => json(doc)
    }
  }

  implicit object BSONDateTimeDecoder extends PartialDecoder[BSONDateTime] {
    private def asLong(dateField: Json): Option[Long] = for {
      number <- dateField.asNumber
      long <- number.toLong
    } yield long

    private def asObjLong(dateField: Json): Option[Long] = for {
      obj <- dateField.asObject
      field <- obj(f"$$numberLong")
      long <- asLong(field)
    } yield long

    val partial: Json => Option[Decoder.Result[BSONDateTime]] = json =>
      for {
        obj <- json.asObject
        dateField <- obj(f"$$date")
        value <- asLong(dateField).orElse(asObjLong(dateField))
      } yield Right(BSONDateTime(value))

  }

  implicit object BSONDateTimeEncoder extends PartialEncoder[BSONDateTime] {
    val partial: PartialFunction[BSONValue, Json] = {
      case dt: BSONDateTime => Json.obj(f"$$date" -> Json.fromLong(dt.value))
    }
  }

  implicit object BSONTimestampDecoder extends PartialDecoder[BSONTimestamp] {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def time(json: Json): Option[(Long, Int)] = (for {
      time <- json.hcursor.downField(f"$$time").as[Long]
      i <- json.hcursor.downField(f"$$i").as[Int]
    } yield (time, i)).right.map(Some(_)).getOrElse(legacy(json))

    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    private def legacy(json: Json): Option[(Long, Int)] = (for {
      time <- json.hcursor.downField(f"$$timestamp").downField("t").as[Long]
      i <- json.hcursor.downField(f"$$timestamp").downField("i").as[Int]
    } yield (time, i)).right.map(Some(_)).getOrElse(None)

    val partial: Json => Option[Decoder.Result[BSONTimestamp]] =
      time(_).map { case (time, i) => Right(BSONTimestamp(time, i)) }

  }

  implicit object BSONTimestampEncoder extends PartialEncoder[BSONTimestamp] {
    val partial: PartialFunction[BSONValue, Json] = {
      case ts: BSONTimestamp => Json.obj(
        f"$$time" -> Json.fromLong(ts.value >>> 32), f"$$i" -> Json.fromInt(ts.value.toInt),
        f"$$timestamp" -> Json.obj(
          "t" -> Json.fromLong(ts.value >>> 32), "i" -> Json.fromInt(ts.value.toInt)
        )
      )
    }
  }

  implicit object BSONArrayDecoder extends PartialDecoder[BSONArray] {
    import cats.instances.either._
    import cats.instances.list._
    import cats.syntax.traverse._

    val partial: Json => Option[Decoder.Result[BSONArray]] =
      _.asArray.map(_.map(toBSON(_)).toList.sequence.right.map(values => BSONArray(values)))

  }

  implicit object BSONArrayEncoder extends PartialEncoder[BSONArray] {
    val partial: PartialFunction[BSONValue, Json] = {
      case array: BSONArray => Json.arr(array.values.map(toJSON(_)): _*)
    }
  }

  def toBSON(json: Json): Decoder.Result[BSONValue] = readAsBSONValue(json)

  @SuppressWarnings(Array("MaxParameters"))
  def readAsBSONValue(json: Json)(
    implicit
    string: PartialDecoder[BSONString],
    objectID: PartialDecoder[BSONObjectID],
    // javascript: PartialDecoder[BSONJavaScript],
    dateTime: PartialDecoder[BSONDateTime],
    timestamp: PartialDecoder[BSONTimestamp],
    // binary: PartialDecoder[BSONBinary],
    // regex: PartialDecoder[BSONRegex],
    double: PartialDecoder[BSONDouble],
    integer: PartialDecoder[BSONInteger],
    long: PartialDecoder[BSONLong],
    boolean: PartialDecoder[BSONBoolean],
    // minKey: PartialDecoder[BSONMinKey.type],
    // maxKey: PartialDecoder[BSONMaxKey.type],
    bnull: PartialDecoder[BSONNull.type],
    // symbol: PartialDecoder[BSONSymbol],
    array: PartialDecoder[BSONArray],
    doc: PartialDecoder[BSONDocument],
    undef: PartialDecoder[BSONUndefined.type]
  ): Decoder.Result[BSONValue] =

    Seq(
      string.partial,
      objectID.partial,
      // javascript,
      dateTime.partial,
      timestamp.partial,
      // binary,
      // regex,
      integer.partial,
      long.partial,
      boolean.partial,
      // minKey,
      // maxKey,
      bnull.partial,
      // symbol,
      array.partial,
      doc.partial,
      undef.partial
    ).foldLeft(Option.empty[Decoder.Result[BSONValue]]) {
      case (result @ Some(_), _) => result
      case (None, partial) => partial(json)
    } match {
      case Some(result) => result
      case None => Left(DecodingFailure(s"unhandled json value: $json", Nil))
    }

  def toJSON(bson: BSONValue): Json = writeAsJsValue(bson)

  @SuppressWarnings(Array("MaxParameters"))
  def writeAsJsValue(bson: BSONValue)(
    implicit
    string: PartialEncoder[BSONString],
    objectID: PartialEncoder[BSONObjectID],
    // javascript: PartialEncoder[BSONJavaScript],
    dateTime: PartialEncoder[BSONDateTime],
    timestamp: PartialEncoder[BSONTimestamp],
    // binary: PartialEncoder[BSONBinary],
    // regex: PartialEncoder[BSONRegex],
    double: PartialEncoder[BSONDouble],
    integer: PartialEncoder[BSONInteger],
    long: PartialEncoder[BSONLong],
    boolean: PartialEncoder[BSONBoolean],
    // minKey: PartialEncoder[BSONMinKey.type],
    // maxKey: PartialEncoder[BSONMaxKey.type],
    bnull: PartialEncoder[BSONNull.type],
    // symbol: PartialEncoder[BSONSymbol],
    array: PartialEncoder[BSONArray],
    doc: PartialEncoder[BSONDocument],
    undef: PartialEncoder[BSONUndefined.type]
  ): Json = string.partial.
    orElse(objectID.partial).
    // orElse(javascript.partial).
    orElse(dateTime.partial).
    orElse(timestamp.partial).
    // orElse(binary.partial).
    // orElse(regex.partial).
    orElse(double.partial).
    orElse(integer.partial).
    orElse(long.partial).
    orElse(boolean.partial).
    // orElse(minKey.partial).
    // orElse(maxKey.partial).
    orElse(bnull.partial).
    // orElse(symbol.partial).
    orElse(array.partial).
    orElse(doc.partial).
    orElse(undef.partial).
    lift(bson).getOrElse(
      throw new json.JSONException(s"Unhandled json value: $bson")
    )
}

object ImplicitBSONHandlers extends ImplicitBSONHandlers

/**
 * Implicit BSON Handlers (BSONDocumentReader/BSONDocumentWriter for JsObject)
 */
sealed trait ImplicitBSONHandlers extends BSONFormats {

  implicit object JsonObjectWriter extends BSONDocumentWriter[JsonObject] {
    def write(obj: JsonObject): BSONDocument =
      BSONFormats.BSONDocumentDecoder.decodeJson(Json.fromJsonObject(obj)).right.get
  }

  implicit object JsonObjectReader extends BSONDocumentReader[JsonObject] {
    def read(document: BSONDocument) =
      BSONFormats.BSONDocumentEncoder(document).as[JsonObject].right.get
  }

  class ObjectWriter[T]()(implicit enc: Encoder[T]) extends BSONDocumentWriter[T] {
    def write(obj: T): BSONDocument =
      BSONFormats.BSONDocumentDecoder.decodeJson(enc(obj)).right.get
  }

  class ObjectReader[T]()(implicit dec: Decoder[T]) extends BSONDocumentReader[T] {
    def read(document: BSONDocument): T =
      dec.decodeJson(BSONFormats.BSONDocumentEncoder(document)).right.get
  }
}

sealed trait LowerImplicitBSONHandlers {
  import scala.language.implicitConversions
  import reactivemongo.bson.{ BSONElement, Producer }

  implicit def jsWriter[A <: Json, B <: BSONValue] = BSONWriter[A, B] { js =>
    (BSONFormats.toBSON(js): @unchecked) match {
      case Right(b: B @unchecked) => b
      case Left(error) => sys.error(s"fails to convert to BSON: $error")
    }
  }

  @SuppressWarnings(Array("MethodNames"))
  implicit def JsFieldBSONElementProducer[T <: Json](jsField: (String, T)): Producer[BSONElement] =
    Producer.element2Producer(BSONElement(jsField._1, BSONFormats.toBSON(jsField._2).right.get))

  implicit object BSONValueReads extends Decoder[BSONValue] {
    def apply(cursor: HCursor) = BSONFormats.toBSON(cursor.value)
  }

  implicit object BSONValueWrites extends Encoder[BSONValue] {
    def apply(bson: BSONValue) = BSONFormats.toJSON(bson)
  }
}
