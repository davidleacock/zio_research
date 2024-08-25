package http

import domain.User
import repo.UserRepository
import zio._
import zio.http._
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

object UserServer {

  private def routes: Routes[UserRepository, Response] =
    Routes(
      Method.GET / "test" -> handler(Response.text("test worked.")),

      // TODO: Test this endpoint
      Method.POST / "users" -> handler { (req: Request) =>
        for {
          user <- req.body.to[User].orElseFail(Response.badRequest)
          response <- ZIO
            .serviceWithZIO[UserRepository](_.create(user))
            .mapBoth(
              err => Response.internalServerError(s"Problem ${err.getMessage}"),
              user => Response.text(user.toString)
            )
        } yield response
      }
    )

  val server: ZIO[UserRepository with Server, Throwable, Nothing] =
    Server.serve(routes)
}
