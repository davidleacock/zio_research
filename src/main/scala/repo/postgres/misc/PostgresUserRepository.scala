package repo.postgres.misc

//package repo.postgres
//
//import domain.User
//import zio._
//import io.getquill._
//import repo.UserRepository
//
//import javax.sql.DataSource
//
//class PostgresUserRepository(ctx: PostgresJdbcContext[SnakeCase]) extends UserRepository[DataSource] {
//  import ctx._
//
////  private val userSchema = quote {
////    querySchema[User]("users")
////  }
//
//  // TODO All this needs to be tested
//
//  override def create(user: User): ZIO[DataSource, Throwable, Unit] =
//    ZIO.serviceWithZIO[DataSource] { _ =>
//      ZIO
//        .fromFuture { implicit ec =>
//          ctx.run(query[User].insertValue(lift(user)))
//        }
//        .mapError(_.getCause)
//        .unit
//    }
//
//  override def get(id: String): ZIO[DataSource, Throwable, Option[User]] =
//    ZIO.serviceWithZIO[DataSource] { _ =>
//      ZIO
//        .fromFuture { implicit ec =>
//          ctx.run(query[User].filter(_.id == lift(id)))
//        }
//        .mapError(_.getCause)
//    }
//
//  override def list(): ZIO[DataSource, Throwable, List[User]] =
//    ZIO.serviceWithZIO[DataSource] { _ =>
//      ZIO
//        .fromFuture { implicit ec =>
//          ctx.run(query[User])
//        }
//        .mapError(_.getCause)
//    }
//}
//
//object PostgresUserRepository {
//  def layer: ZLayer[DataSource, Throwable, UserRepository[DataSource]] = ZLayer.fromFunction { ds: DataSource =>
//    val ctx: PostgresJdbcContext[SnakeCase] = new PostgresJdbcContext(SnakeCase, ds)
//    new PostgresUserRepository(ctx)
//  }
//}