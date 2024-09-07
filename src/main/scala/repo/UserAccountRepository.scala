package repo

import domain.UserAccount
import zio._

trait UserAccountRepository {
  def create(user: UserAccount): ZIO[Any, Throwable, Unit]
  def get(id: String): ZIO[Any, Throwable, Option[UserAccount]]
  def list(): ZIO[Any, Throwable, List[UserAccount]]
}
