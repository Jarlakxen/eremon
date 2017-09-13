package io.eremon.criteria

import scala.language.dynamics

/**
 * The '''Untyped''' type defines the behaviour expected of queries where the
 * MongoDB document may not correspond to a Scala type known to the system
 * using this abstraction.
 */
sealed trait Untyped
  extends Dynamic {
  def selectDynamic(field: String): Term[Any] = Term[Any](field);
}

object Untyped {
  /**
   * The criteria property is a ''factory'' of '''Untyped''' instances.
   */
  val criteria = new Untyped {};

  def where(block: (Untyped) => Expression): Expression = block(criteria);

  def where(block: (Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);

  def where(block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression): Expression =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria);
}

