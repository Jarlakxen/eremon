package io.eremon

package object exceptions {

  case class MissingDatabaseName(uri: String) extends RuntimeException(s"Missing database name in uri $uri")

}