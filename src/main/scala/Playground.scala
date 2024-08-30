import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object Playground extends ZIOAppDefault {

  /*
      ZIO.scoped manages the lifecycle of resources within a scope. Any resource acquired within .scoped
      are automatically released when the block exists, either normally or due to error.
   */

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIO.scoped {
      for {
        resource <- ZIO.acquireRelease(
          ZIO.succeed(println("Acquiring resource..."))
        )(_ => ZIO.succeed(println("Releasing resource...")))
        _ <- ZIO.succeed(println(s"Using $resource"))
      } yield ()
    }
}
