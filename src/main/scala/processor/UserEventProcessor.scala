package processor

import consumer.IngressUserConsumer
import domain.UserError
import domain.UserError.{ConsumerError, PersistenceError}
import repo.UserRepository
import zio._

object UserEventProcessor {

  def processStream: ZIO[IngressUserConsumer with UserRepository, UserError, Unit] =
    for {
      consumer <- ZIO.service[IngressUserConsumer]
      repo <- ZIO.service[UserRepository]
      _ <- consumer
        .consume
        .mapError(err => ConsumerError(err))
        .mapZIO { user =>
          repo.create(user)
            .mapError(err => PersistenceError(err.getMessage))
        }
        .runDrain
    } yield ()
}
