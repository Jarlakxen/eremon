package io.eremon.criteria

import scala.reflect.macros.blackbox.Context
/**
 * The '''TypedMacros''' `object` defines the
 * [[http://docs.scala-lang.org/overviews/macros/overview.html Scala Macros]]
 * used in supporting type-checked
 * [[reactivemongo.extensions.dsl.criteria.Term]] creation.
 *
 * @author svickers
 *
 */
object TypedMacros {
  /// Class Imports
  import Typed.PropertyAccess

  def createTerm[T <: AnyRef: c.WeakTypeTag, U: c.WeakTypeTag](c: Context { type PrefixType = PropertyAccess[T] })(statement: c.Tree): c.Tree =
    {
      import c.universe._

      val q"""(..$args) => $select""" = statement;

      val selectors = select.collect {
        case Select(_, TermName(property)) => property;
      }.reverse.mkString(".");

      val propertyType = weakTypeOf[U];

      q"""new Term[${propertyType}] (${selectors})"""
    }
}

