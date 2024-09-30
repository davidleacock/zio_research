package domain

import zio.json._
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.{BinaryCodec, JsonCodec}

final case class UserAccount(id: String, name: String)

object UserAccount {
  implicit val decoder: JsonDecoder[UserAccount] = DeriveJsonDecoder.gen[UserAccount]
  implicit val encoder: JsonEncoder[UserAccount] = DeriveJsonEncoder.gen[UserAccount]
  implicit val schema: Schema[UserAccount] = DeriveSchema.gen[UserAccount]
  implicit val codec: BinaryCodec[UserAccount] = JsonCodec.schemaBasedBinaryCodec(schema)
}

//sealed trait UserError extends Throwable
//object UserError {
//  case class InvalidDataError(message: String) extends UserError
//  case class PersistenceError(message: String) extends UserError
//  case class ConsumerError(cause: Throwable) extends UserError
//}
