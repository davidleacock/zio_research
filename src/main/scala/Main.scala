import zio._

object Main extends ZIOAppDefault {

//  override def run: ZIO[Any, Throwable, Nothing] = {
//    val userRepositoryLayer = InMemoryUserAccountRepository.layer(Map.empty[String, UserAccount])
//    val serverLayer = Server.defaultWithPort(8080)
//
//    UserAccountServer.server.provide(userRepositoryLayer, serverLayer)
//  }

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = ???
}
