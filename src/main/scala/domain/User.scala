package domain

import zio.json._
import zio.schema.{DeriveSchema, Schema}

case class User(id: String, name: String)

object User {
  implicit val decoder: JsonDecoder[User] = DeriveJsonDecoder.gen[User]
  implicit val encoder: JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  implicit val schemer: Schema[User] = DeriveSchema.gen[User]
}

sealed trait UserError extends Throwable
object UserError {
  case class InvalidDataError(message: String) extends UserError
  case class PersistenceError(message: String) extends UserError
  case class ConsumerError(cause: Throwable) extends UserError
}

