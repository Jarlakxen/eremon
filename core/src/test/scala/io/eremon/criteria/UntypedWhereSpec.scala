package io.eremon.criteria

import org.scalatest._

import reactivemongo.bson._

/**
 * The '''UntypedWhereSpec''' type verifies the behaviour expected of the
 * `where` method in the [[reactivemongo.extensions.dsl.criteria.Untyped]]
 * `type`.
 *
 * @author svickers
 *
 */
class UntypedWhereSpec
  extends FlatSpec
  with Matchers {
  /// Class Imports
  import Untyped._

  "An Untyped where" should "support 1 placeholder" in
    {
      val q = where {
        _.a === 1
      }

      BSONDocument.pretty(q) shouldBe (
        BSONDocument.pretty(
          BSONDocument(
            "a" -> BSONInteger(1))));
    }

  it should "support 2 placeholders" in
    {
      val q = where {
        _.a === 1 && _.b === 2
      }

      BSONDocument.pretty(q) shouldBe (
        BSONDocument.pretty(
          BSONDocument(
            "$and" ->
              BSONArray(
                BSONDocument(
                  "a" -> BSONInteger(1)),
                BSONDocument(
                  "b" -> BSONInteger(2))))));
    }

  it should "support 3 placeholders" in
    {
      val q = where {
        _.a === 1 && _.b === 2 && _.c === 3
      }

      BSONDocument.pretty(q) shouldBe (
        BSONDocument.pretty(
          BSONDocument(
            "$and" ->
              BSONArray(
                BSONDocument(
                  "a" -> BSONInteger(1)),
                BSONDocument(
                  "b" -> BSONInteger(2)),
                BSONDocument(
                  "c" -> BSONInteger(3))))));
    }

  /// The library supports from 1 to 22 placeholders for the where method.
  it should "support 22 placeholders" in
    {
      val q = where {
        _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0 &&
          _.p === 0
      }

      BSONDocument.pretty(q) shouldBe (
        BSONDocument.pretty(
          BSONDocument(
            "$and" ->
              BSONArray(List.fill(22)(BSONDocument("p" -> BSONInteger(0)))))));
    }
}