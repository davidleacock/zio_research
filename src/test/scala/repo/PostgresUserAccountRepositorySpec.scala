package repo

import domain.UserAccount
import io.getquill.jdbczio.Quill
import io.getquill.{Literal, Update}
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import repo.impl.{DataService, PostgresUserAccountRepositoryLive}
import zio._
import zio.test.Assertion._
import zio.test._

import javax.sql.DataSource

object PostgresUserAccountRepositorySpec extends ZIOSpecDefault {

  val postgresContainer: ZLayer[Any, Throwable, PostgreSQLContainer[Nothing]] =
    ZLayer.scoped {
      ZIO.acquireRelease {
        ZIO.attempt {
          val container = new PostgreSQLContainer[Nothing](DockerImageName.parse("postgres:15.3"))
          container.start()
          container
        }
      }(container => ZIO.succeed(container.stop()))
    }

  val dataSourceLive: ZLayer[PostgreSQLContainer[Nothing], Throwable, DataSource] =
    ZLayer.fromZIO {
      ZIO.serviceWith[PostgreSQLContainer[Nothing]] { container =>
        val dataSource = new PGSimpleDataSource()
        dataSource.setURL(container.getJdbcUrl)
        dataSource.setUser(container.getUsername)
        dataSource.setPassword(container.getPassword)
        dataSource
      }
    }

  val postgresLive: ZLayer[DataSource, Nothing, Quill.Postgres[Literal]] =
    Quill.Postgres.fromNamingStrategy(Literal)

  val dataServiceLive: ZLayer[Quill.Postgres[Literal], Nothing, DataService] = ZLayer.fromFunction(DataService.apply _)

  val userRepositoryLayer: ZLayer[Quill.Postgres[Literal], Nothing, PostgresUserAccountRepositoryLive] =
    dataServiceLive >>> ZLayer.fromFunction(PostgresUserAccountRepositoryLive.apply _)

  val schemaInitialization: ZIO[Quill.Postgres[Literal], Throwable, Unit] = {
    ZIO.serviceWithZIO[Quill.Postgres[Literal]] { ctx =>
      import ctx._
      val createSchema = quote {
        sql"""
          CREATE TABLE IF NOT EXISTS "useraccount" (
            id VARCHAR(255) PRIMARY KEY,
            name VARCHAR(255) NOT NULL
          )
        """.as[Update[UserAccount]]
      }
      ctx.run(createSchema).unit
    }
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("PostgresUserRepository")(
    test("create user entry") {
      for {
        repo <- ZIO.service[PostgresUserAccountRepositoryLive]
        _ <- schemaInitialization
        user1 = UserAccount("1", "UserName")
        _ <- repo.create(user1)
        user2 = UserAccount("2", "UserName2")
        _ <- repo.create(user2)
        users <- repo.list()
      } yield assert(users)(contains(user1) && contains(user2))
    }
  ).provideLayerShared(
    postgresContainer >>>
      dataSourceLive >>>
      postgresLive >>>
      (userRepositoryLayer ++ ZLayer.service[Quill.Postgres[Literal]])
  )
}
