package repo.postgres

//import domain._
import domain.User
import zio.schema.Schema
//import zio.postgres._
//import zio.postgres.implicits._
//import domain.User.scheme
import zio.sql.postgresql._
import zio.sql._
import zio.sql.postgresql.PostgresJdbcModule
//import sviezypan.domain.AppError.RepositoryError
//import zio.schema.DeriveSchema
import zio.schema.{ DeriveSchema, Schema }

trait PostgresTableDescription extends PostgresJdbcModule {

  val users = defineTable[User]

  val (id, name) = users.columns
}
