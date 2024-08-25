import http._
import zio._

object Main extends ZIOAppDefault {

  override def run =
    UserServer.server
}
