package server

import domain.UserAccount
import repo.UserAccountRepository
import zio._
import zio.http._

object UserAccountServer {

  private def routes: Routes[UserAccountRepository, Response] =
    Routes(
      Method.GET / "users" / string("id") -> handler { (id: String, _: Request) =>
        ZIO
          .serviceWithZIO[UserAccountRepository](_.get(id))
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
          .serviceWithZIO[UserAccountRepository](_.list())
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
          user <- req.body.to[UserAccount].orElseFail(Response.badRequest)
          response <- ZIO
            .serviceWithZIO[UserAccountRepository](_.create(user))
            .mapBoth(
              err => Response.internalServerError(s"Problem ${err.getMessage}"),
              user => Response.text(user.toString)
            )
        } yield response
      },
      Method.GET / "test" -> handler(Response.text("HTTP Server online..."))
    )

  // TODO - Do I need to make a layer builder here or something? So I return a Server rather than this runnable
  val server: ZIO[UserAccountRepository with Server, Throwable, Nothing] =
    Server.serve(routes)
}
