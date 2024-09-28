package processor

import consumer.IngressUserAccountConsumer
import domain.UserAccount
import repo.UserAccountRepository
import zio._
import zio.stream.ZStream

case class EventProcessorSuccess(userAccount: UserAccount)
case class EventProcessorFail(reason: String)

object UserEventProcessorStream {

  def processStream
    : ZStream[IngressUserAccountConsumer with UserAccountRepository, EventProcessorFail, EventProcessorSuccess] =
    ZStream.unwrap {
      for {
        consumer <- ZIO.service[IngressUserAccountConsumer]
        repo <- ZIO.service[UserAccountRepository]
      } yield consumer
        .consume
        .mapError(err => EventProcessorFail(s"Error processing from ingress stream: ${err.reason}"))
        .mapZIO { user =>
          repo
            .create(user)
            .mapBoth(
              err => EventProcessorFail(s"Error persisting to user account repo: ${err.getMessage}"),
              _ => EventProcessorSuccess(user)
            )
        }
    }
}
