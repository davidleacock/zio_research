import http._
import repo.InMemoryUserRepository
import zio._
import zio.http.Server

object Main extends ZIOAppDefault {

  override def run: ZIO[Any, Throwable, Nothing] = {
    val userRepositoryLayer = InMemoryUserRepository.layer
    val serverLayer = Server.defaultWithPort(8080)

    UserServer.server.provide(userRepositoryLayer, serverLayer)
  }
}
