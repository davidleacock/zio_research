package domain

import zio.json._
import zio.schema.{DeriveSchema, Schema}

final case class User(id: String, name: String)

object User {
  implicit val decoder: JsonDecoder[User] = DeriveJsonDecoder.gen[User]
  implicit val encoder: JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  implicit val schemeRecord: Schema.Record[User] = DeriveSchema.gen[User].asInstanceOf[Schema.Record[User]]
}

sealed trait UserError extends Throwable
object UserError {
  case class InvalidDataError(message: String) extends UserError
  case class PersistenceError(message: String) extends UserError
  case class ConsumerError(cause: Throwable) extends UserError
}

