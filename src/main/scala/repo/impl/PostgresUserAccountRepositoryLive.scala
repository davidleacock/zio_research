package repo.impl

import domain.UserAccount
import io.getquill._
import io.getquill.jdbczio.Quill
import repo.UserAccountRepository
import zio._

import java.sql.SQLException

case class DataService(quill: Quill.Postgres[Literal]) {
  import quill._
  val users = quote(query[UserAccount])
  val userById = quote((id: String) => users.filter(_.id == id))
  val insertUser = quote((user: UserAccount) => users.insertValue(user))
}

case class PostgresUserAccountRepositoryLive(dataService: DataService) extends UserAccountRepository {
  import dataService.quill._

  override def create(user: UserAccount): ZIO[Any, Throwable, Unit] = run(dataService.insertUser(lift(user))).unit

  override def get(id: String): ZIO[Any, SQLException, Option[UserAccount]] = run(dataService.userById(lift(id))).map(_.headOption)

  override def list(): ZIO[Any, SQLException, List[UserAccount]] = run(dataService.users)
}