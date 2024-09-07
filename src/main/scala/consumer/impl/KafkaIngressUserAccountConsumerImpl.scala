package consumer.impl

import consumer.IngressUserAccountConsumer
import domain.UserAccount
import zio._
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

case class KafkaIngressUserAccountConsumerImpl(consumer: Consumer) extends IngressUserAccountConsumer {

  // TODO Throw custom error?
  override def consume: ZStream[Any, Throwable, UserAccount] =
    consumer
      .plainStream(Subscription.topics("user-events"), Serde.string, Serde.string)
      .tap(e => Console.printLine(s"Received record: ${e.value}"))
      .mapZIO { record =>
        ZIO
          .fromEither(UserAccount.decoder.decodeJson(record.value))
          .mapError(err => new RuntimeException(s"Failed to decode user: $err"))
      }
      .tapError(err => Console.printLine(s"Stream error: $err"))
}

object KafkaIngressUserAccountConsumerImpl {
  def layer: ZLayer[Consumer, Throwable, IngressUserAccountConsumer] =
    ZLayer.fromFunction(KafkaIngressUserAccountConsumerImpl.apply _)
}
