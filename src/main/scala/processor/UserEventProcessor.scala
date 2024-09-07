package processor

import consumer.IngressUserAccountConsumer
import domain.UserError
import domain.UserError.{ConsumerError, PersistenceError}
import repo.UserAccountRepository
import zio._

object UserEventProcessor {

  def processStream: ZIO[IngressUserAccountConsumer with UserAccountRepository, UserError, Unit] =
    for {
      consumer <- ZIO.service[IngressUserAccountConsumer]
      repo <- ZIO.service[UserAccountRepository]
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
