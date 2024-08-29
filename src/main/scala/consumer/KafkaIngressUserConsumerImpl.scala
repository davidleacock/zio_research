package consumer

import domain.User
import zio._
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

case class KafkaIngressUserConsumerImpl(consumer: Consumer) extends IngressUserConsumer {

  // TODO - Currently this will just log whatever we consume, but we need to do something with the User
  // TODO need to test this with a Kafka Container

  // TODO do I need the additional explicit drain and offset commits? Does it happen by default?
  // TODO how do I pass in the topic information?
  override def consume: ZStream[Any, Throwable, User] =
    consumer
      .plainStream(Subscription.topics("user-events"), Serde.string, Serde.string)
      .tap(e => Console.printLine(s"Got a record.... ${e.value}"))
      .mapZIO { record =>
        ZIO
          .fromEither(zio.json.JsonDecoder[User].decodeJson(record.value))
          .mapError(err => new RuntimeException(s"Failed to decode user: $err"))
      }
}

object KafkaIngressUserConsumerImpl {
  def layer(settings: ConsumerSettings): ZLayer[Consumer, Throwable, IngressUserConsumer] =
    // TODO wtf is Scoped?
    ZLayer.scoped(Consumer.make(settings)) >>>
      ZLayer.fromFunction(consumer => KafkaIngressUserConsumerImpl(consumer))
}
