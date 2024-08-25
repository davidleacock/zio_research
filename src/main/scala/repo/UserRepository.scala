package repo

import domain.User
import zio._

trait UserRepository {
  def create(user: User): ZIO[Any, Throwable, Unit]
  def get(id: String): ZIO[Any, Throwable, Option[User]]
  def list(): ZIO[Any, Throwable, List[User]]
}
