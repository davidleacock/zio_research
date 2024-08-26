package processor

import consumer.UserConsumer
import repo.UserRepository
import zio._

object UserEventProcessor {

  // TODO rewrite using for comp? How can I make this clearner?
  // TODO test
  def processStream: ZIO[UserConsumer with UserRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[UserConsumer](_.consume.mapZIO { user =>
      ZIO
        .serviceWithZIO[UserRepository](_.create(user))
        .tapError(err => Console.printLine(s"Failed to persist user: $err"))
    }.runDrain)
}
