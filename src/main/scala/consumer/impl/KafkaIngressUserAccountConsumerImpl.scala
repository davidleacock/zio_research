package consumer.impl

import consumer.{IngressUserAccountConsumer, IngressUserAccountError}
import domain.UserAccount
import zio._
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

case class KafkaIngressUserAccountConsumerImpl(consumer: Consumer) extends IngressUserAccountConsumer {

  // TODO use configs to read topic
  override def consume: ZStream[Any, IngressUserAccountError, UserAccount] =
    consumer
      .plainStream(Subscription.topics("user-events"), Serde.string, Serde.string)
      .tap(e => Console.printLine(s"Received record: ${e.value}"))
      .mapZIO { record =>
        ZIO
          .fromEither(UserAccount.decoder.decodeJson(record.value))
          .mapError(err => IngressUserAccountError(s"Failed to create UserAccount: $err"))
      }
      .mapError(err => IngressUserAccountError(s"Kafka ingress error: $err"))
}

object KafkaIngressUserAccountConsumerImpl {
  def layer: ZLayer[Consumer, Throwable, IngressUserAccountConsumer] =
    ZLayer.fromFunction(KafkaIngressUserAccountConsumerImpl.apply _)
}
