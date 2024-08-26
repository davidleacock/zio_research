package http

import domain.User
import repo.UserRepository
import zio._
import zio.http._
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

object UserServer {

  private def routes: Routes[UserRepository, Response] =
    Routes(
      Method.GET / "users" / string("id") -> handler { (id: String, _: Request) =>
        ZIO
          .serviceWithZIO[UserRepository](_.get(id))
          .mapBoth(
            err => Response.internalServerError(s"Problem ${err.getMessage}"),
            {
              case Some(user) => Response.text(s"User: $user")
              case None       => Response.notFound(s"User $id not found.")
            }
          )
      },
      Method.GET / "users" -> handler { (_: Request) =>
        ZIO
          .serviceWithZIO[UserRepository](_.list())
          .mapBoth(
            err => Response.internalServerError(s"Problem ${err.getMessage}"),
            users => {
              val response = users.map(user => s"ID: ${user.id} Name: ${user.name}").mkString("\n")
              Response.text(response)
            }
          )
      },
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
      },
      Method.GET / "test" -> handler(Response.text("HTTP Server online..."))
    )

  val server: ZIO[UserRepository with Server, Throwable, Nothing] =
    Server.serve(routes)
}
