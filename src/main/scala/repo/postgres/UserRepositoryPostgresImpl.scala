package repo.postgres

import domain.User
import repo.UserRepository
import zio.{ZIO, ZLayer}
//import zio.sql.ConnectionPool
import zio.sql._

class UserRepositoryPostgresImpl(pool: ConnectionPool) extends UserRepository with PostgresTableDescription {

  private lazy val driverLayer: ZLayer[Any, Nothing, SqlDriver] =
    ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  override def create(user: User): ZIO[Any, Throwable, Unit] = {
    val query = insertInto(users)(id, name).values((user.id, user.name))

    ZIO.logInfo(s"Query to insert User is ${renderInsert(query)}") *>
      execute(query).provide(driverLayer).unit
  }

  override def get(userId: String): ZIO[Any, Throwable, Option[User]] = {
    val query = {
      select(id, name).from(users).where(id == userId)
    }

   ???
  }


  override def list(): ZIO[Any, Throwable, List[User]] = ???
}
