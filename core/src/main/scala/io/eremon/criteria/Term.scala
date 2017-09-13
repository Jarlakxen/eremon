package io.eremon.criteria

import scala.language.dynamics

import reactivemongo.bson._

/**
 * A '''Term'' instance reifies the use of a MongoDB document field, both
 * top-level or nested.  Operators common to all ''T'' types are defined here
 * with type-specific ones provided in the companion object below.
 *
 * @author svickers
 *
 */
final case class Term[T](`_term$name`: String)
  extends Dynamic {
  /**
   * Logical equality.
   */
  def ===[U <: T: ValueBuilder](rhs: U): Expression = Expression(
    `_term$name`,
    `_term$name` -> implicitly[ValueBuilder[U]].bson(rhs));

  /**
   * Logical equality.
   */
  def @==[U <: T: ValueBuilder](rhs: U): Expression = ===[U](rhs);

  /**
   * Logical inequality: '''$ne'''.
   */
  def <>[U <: T: ValueBuilder](rhs: U): Expression = Expression(
    `_term$name`,
    "$ne" -> implicitly[ValueBuilder[U]].bson(rhs));

  /**
   * Logical inequality: '''$ne'''.
   */
  def =/=[U <: T: ValueBuilder](rhs: U): Expression = <>[U](rhs);

  /**
   * Logical inequality: '''$ne'''.
   */
  def !==[U <: T: ValueBuilder](rhs: U): Expression = <>[U](rhs);

  /**
   * Less-than comparison: '''$lt'''.
   */
  def <[U <: T: ValueBuilder](rhs: U): Expression = Expression(
    `_term$name`,
    "$lt" -> implicitly[ValueBuilder[U]].bson(rhs));

  /**
   * Less-than or equal comparison: '''$lte'''.
   */
  def <=[U <: T: ValueBuilder](rhs: U): Expression = Expression(
    `_term$name`,
    "$lte" -> implicitly[ValueBuilder[U]].bson(rhs));

  /**
   * Greater-than comparison: '''$gt'''.
   */
  def >[U <: T: ValueBuilder](rhs: U): Expression = Expression(
    `_term$name`,
    "$gt" -> implicitly[ValueBuilder[U]].bson(rhs));

  /**
   * Greater-than or equal comparison: '''$gte'''.
   */
  def >=[U <: T: ValueBuilder](rhs: U): Expression = Expression(
    `_term$name`,
    "$gte" -> implicitly[ValueBuilder[U]].bson(rhs));

  /**
   * Field existence: '''$exists'''.
   */
  def exists: Expression = Expression(
    `_term$name`,
    "$exists" -> BSONBoolean(true));

  /**
   * Field value equals one of the '''values''': '''$in'''.
   */
  def in[U <: T: ValueBuilder](values: Traversable[U])(implicit B: ValueBuilder[U]): Expression = Expression(
    `_term$name`,
    "$in" -> BSONArray(values map (B.bson)));

  /**
   * Field value equals either '''head''' or one of the (optional)
   * '''tail''' values: '''$in'''.
   */
  def in[U <: T: ValueBuilder](head: U, tail: U*)(implicit B: ValueBuilder[U]): Expression = Expression(
    `_term$name`,
    "$in" -> BSONArray(Seq(B.bson(head)) ++ tail.map(B.bson)));

  def selectDynamic[U](field: String): Term[U] = Term[U](
    `_term$name` + "." + field);
}

object Term {
  /// Class Types
  /**
   * The '''CollectionTermOps''' `implicit` provides EDSL functionality to
   * `Seq` [[reactivemongo.extensions.dsl.criteria.Term]]s only.
   */
  implicit class CollectionTermOps[T](val term: Term[Seq[T]])
    extends AnyVal {
    def all(values: Traversable[T])(implicit B: ValueBuilder[T]): Expression = Expression(
      term.`_term$name`,
      "$all" -> BSONArray(values map (B.bson)));
  }

  /**
   * The '''StringTermOps''' `implicit` enriches
   * [[reactivemongo.extensions.dsl.criteria.Term]]s for
   * `String`-only operations.
   */
  implicit class StringTermOps[T >: String](val term: Term[T])
    extends AnyVal {
    def =~(re: (String, RegexModifier)): Expression = Expression(
      term.`_term$name`,
      "$regex" -> BSONRegex(re._1, re._2.value));

    def =~(re: String): Expression = Expression(
      term.`_term$name`,
      "$regex" -> BSONRegex(re, ""));

    def !~(re: (String, RegexModifier)): Expression = Expression(
      term.`_term$name`,
      "$not" -> BSONDocument("$regex" -> BSONRegex(re._1, re._2.value)));

    def !~(re: String): Expression = Expression(
      term.`_term$name`,
      "$not" -> BSONDocument("$regex" -> BSONRegex(re, "")));
  }
}

/**
 * '''RegexModifier''' types provide the ability for developers to specify
 * `$regex` modifiers using type-checked Scala types.  For example, specifying
 * a `$regex` which ignores case for the `surname` property can be written as:
 *
 * {{{
 *
 * criteria.surname =~ "smith" -> IgnoreCase;
 *
 * }}}
 *
 * Multiple modifiers can be combined using the or (`|`) operator,
 * producing an implementation-defined ordering.
 *
 * @author svickers
 *
 */
sealed trait RegexModifier {
  /**
   * Use the or operator to combine two or more '''RegexModifier'''s into
   * one logical value.
   */
  def |(other: RegexModifier): RegexModifier =
    CombinedRegexModifier(this, other);

  def value(): String;
}

case class CombinedRegexModifier(
  lhs: RegexModifier,
  rhs: RegexModifier)
  extends RegexModifier {
  override def value(): String = lhs.value + rhs.value;
}

case object DotMatchesEverything
  extends RegexModifier {
  override val value: String = "s";
}

case object ExtendedExpressions
  extends RegexModifier {
  override val value: String = "x";
}

case object IgnoreCase
  extends RegexModifier {
  override val value: String = "i";
}

case object MultilineMatching
  extends RegexModifier {
  override val value: String = "m";
}

