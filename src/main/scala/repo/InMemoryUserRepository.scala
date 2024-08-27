package repo

import domain.User
import zio._

private class InMemoryUserRepository(ref: Ref[Map[String, User]]) extends UserRepository {

  override def create(user: User): ZIO[Any, Throwable, Unit] =
    ref.update(_ + (user.id -> user)).unit

  override def get(id: String): ZIO[Any, Throwable, Option[User]] =
    ref.get.map(_.get(id))

  override def list(): ZIO[Any, Throwable, List[User]] =
    ref.get.map(_.values.toList)
}

object InMemoryUserRepository {
  def layer(data: Map[String, User]): ULayer[UserRepository] =
    ZLayer.fromZIO(
      Ref.make(data).map(ref => new InMemoryUserRepository(ref))
    )
}
