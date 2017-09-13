package io.eremon

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.slf4j.LoggerFactory

/**
 * Boilerplate remover and preferred testing style.
 */
trait Spec extends FlatSpec
  with Matchers
  with OptionValues
  with Inside
  with Retries
  with TryValues
  with Inspectors
  with TypeCheckedTripleEquals
  with BeforeAndAfter
  with BeforeAndAfterAll { self =>

  val log = LoggerFactory.getLogger(this.getClass)

}