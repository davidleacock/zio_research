package repo.postgres

import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import zio._
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault}
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import zio.test._

object PostgresUserRepositorySpec extends ZIOSpecDefault {

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

  // Do I need Has[]?
  val quillContextLayer: ZLayer[PostgreSQLContainer[Nothing], Nothing, PostgresZioJdbcContext[SnakeCase]] =
    ZLayer.fromZIO {
      ZIO.serviceWith[PostgreSQLContainer[Nothing]] { container =>
        new PostgresZioJdbcContext(SnakeCase) {

          // Return the Postgres impl

          protected val ds = {
            val dataSource = new org.postgresql.ds.PGSimpleDataSource()
            dataSource.setURL(container.getJdbcUrl)
            dataSource.setUser(container.getUsername)
            dataSource.setPassword(container.getPassword)
            dataSource
          }
        }
      }
    }

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("PostgresUserRepository")(
    test("should do something") {
      assertTrue(true)
    }
  ).provideLayerShared(postgresContainer >>> quillContextLayer)
//  ).provideLayerShared(postgresContainer >>> (quillContextLayer ++ ZLayer[PostgresSQLContainer])
}
