package repo

import domain.UserAccount
import io.getquill.Literal
import io.getquill.jdbczio.Quill
import repo.impl.PostgresUserAccountRepositoryLive
import zio._
import zio.test.Assertion._
import zio.test._

object PostgresUserAccountRepositorySpec extends ZIOSpecDefault with TestInfrastructure {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("PostgresUserRepository")(
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
