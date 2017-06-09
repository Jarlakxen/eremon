package io.eremon.criteria

import scala.language.experimental.macros

/**
 * The '''Typed''' `object` provides the ability to ''lift'' an arbitrary type
 * `T` into the [[reactivemongo.extensions.dsl.criteria]] world.  Each property
 * is represented as a [[reactivemongo.extensions.dsl.criteria.Term]].
 *
 * @author svickers
 *
 */
object Typed {
  /// Class Types
  /**
   * The '''PropertyAccess''' type exists for syntactic convenience when
   * the `criteria` method is used.  The tandem allow for constructs such
   * as:
   *
   * {{{
   * import Typed._
   *
   * val typeCheckedQuery = criteria[SomeType] (_.first) < 10 && (
   *    criteria[SomeType] (_.second) >= 20.0 ||
   *    criteria[SomeType] (_.second).in (0.0, 1.0)
   *    );
   * }}}
   *
   * @author svickers
   *
   */
  final class PropertyAccess[ParentT <: AnyRef] {
    def apply[T](statement: ParentT => T): Term[T] = macro TypedMacros.createTerm[ParentT, T];
  }

  /**
   * The criteria method produces a type which enforces the existence of
   * property names within ''T''.
   */
  def criteria[T <: AnyRef]: PropertyAccess[T] = new PropertyAccess[T];
}

