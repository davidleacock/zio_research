package consumer

import domain.User
import zio._
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

case class KafkaIngressUserConsumerImpl(consumer: Consumer) extends IngressUserConsumer {

  // TODO Throw custom error?
  override def consume: ZStream[Any, Throwable, User] =
    consumer
      .plainStream(Subscription.topics("user-events"), Serde.string, Serde.string)
      .tap(e => Console.printLine(s"Received record: ${e.value}"))
      .mapZIO { record =>
        ZIO
          .fromEither(User.decoder.decodeJson(record.value))
          .mapError(err => new RuntimeException(s"Failed to decode user: $err"))
      }
      .tapError(err => Console.printLine(s"Stream error: $err"))
}

object KafkaIngressUserConsumerImpl {
  def layer: ZLayer[Consumer, Throwable, IngressUserConsumer] =
    ZLayer.fromFunction(KafkaIngressUserConsumerImpl.apply _)
}
