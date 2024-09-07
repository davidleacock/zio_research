package repo.impl

import domain.UserAccount
import repo.UserAccountRepository
import zio._

private class InMemoryUserAccountRepository(ref: Ref[Map[String, UserAccount]]) extends UserAccountRepository {

  override def create(user: UserAccount): ZIO[Any, Throwable, Unit] =
    ref.update(_ + (user.id -> user)).unit

  override def get(id: String): ZIO[Any, Throwable, Option[UserAccount]] =
    ref.get.map(_.get(id))

  override def list(): ZIO[Any, Throwable, List[UserAccount]] =
    ref.get.map(_.values.toList)
}

object InMemoryUserAccountRepository {
  def layer(data: Map[String, UserAccount]): ULayer[UserAccountRepository] =
    ZLayer.fromZIO(
      Ref.make(data).map(ref => new InMemoryUserAccountRepository(ref))
    )
}
