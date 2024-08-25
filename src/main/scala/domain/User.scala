package domain

import zio.json._
import zio.schema.{DeriveSchema, Schema}

case class User(id: String, name: String)

object User {
  implicit val decoder: JsonDecoder[User] = DeriveJsonDecoder.gen[User]
  implicit val encoder: JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  implicit val schemer: Schema[User] = DeriveSchema.gen[User]
}


