package processor

import consumer.UserConsumer
import repo.UserRepository
import zio._

object UserEventProcessor {

  // TODO - How does this handle errors?
  def processStream: ZIO[UserConsumer with UserRepository, Throwable, Unit] = {
    for {
      consumer <- ZIO.service[UserConsumer]
      repo <- ZIO.service[UserRepository]
      _ <- consumer.consume.mapZIO { user =>
        repo.create(user).tapError(err => Console.printLine(s"Failed to persist user: $err"))
      }.runDrain
    } yield ()
  }
}
