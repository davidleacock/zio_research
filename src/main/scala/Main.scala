import domain.UserAccount
import http._
import repo.impl.InMemoryUserAccountRepository
import zio._
import zio.http.Server

object Main extends ZIOAppDefault {

  override def run: ZIO[Any, Throwable, Nothing] = {
    val userRepositoryLayer = InMemoryUserAccountRepository.layer(Map.empty[String, UserAccount])
    val serverLayer = Server.defaultWithPort(8080)

    UserAccountServer.server.provide(userRepositoryLayer, serverLayer)
  }
}
