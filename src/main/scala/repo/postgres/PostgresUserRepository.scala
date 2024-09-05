package repo.postgres

import domain.User
import io.getquill._
import io.getquill.jdbczio.Quill
import repo.UserRepository
import zio._

import java.sql.SQLException

// TODO Read config
// TODO Refactor and rename
// TODO create test with Postgres container, and maybe a test config then this this code out

case class DataService(quill: Quill.Postgres[Literal]) {
  import quill._
  val users = quote(query[User])
  val userById = quote((id: String) => users.filter(u => u.id == id))
  val insertUser = quote((user: User) => users.insertValue(user))
}

case class PostgresUserRepository(dataService: DataService) extends UserRepository {
  import dataService.quill._

  override def create(user: User): ZIO[Any, SQLException, Unit] = run(dataService.insertUser(lift(user)))

  override def get(id: String): ZIO[Any, SQLException, Option[User]] = run(dataService.userById(lift(id)))

  override def list(): ZIO[Any, SQLException, List[User]] = run(dataService.users)
}

/*
 object Application {
    def getPeopleByName(name: String) =
      ZIO.serviceWithZIO[ApplicationLive](_.getPeopleByName(name))
    def getAllPeople() =
      ZIO.serviceWithZIO[ApplicationLive](_.getAllPeople())
  }
  case class Person(name: String, age: Int)

  def main(args: Array[String]): Unit = {
    val dataServiceLive = ZLayer.fromFunction(DataService.apply _)
    val applicationLive = ZLayer.fromFunction(ApplicationLive.apply _)
    val dataSourceLive  = Quill.DataSource.fromPrefix("testPostgresDB")
    val postgresLive    = Quill.Postgres.fromNamingStrategy(Literal)

    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe
        .run(
          (for {
            joes      <- Application.getPeopleByName("Joe")
            _         <- printLine(joes)
            allPeople <- Application.getAllPeople()
            _         <- printLine(allPeople)
          } yield ()).provide(applicationLive, dataServiceLive, dataSourceLive, postgresLive)
        )
        .getOrThrow()
    }
    ()



 */