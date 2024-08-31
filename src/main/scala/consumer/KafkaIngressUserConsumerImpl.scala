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
  override def consume: ZStream[Any, Throwable, User] = {
    println("Consume")
    consumer
      .plainStream(Subscription.topics("user-events"), Serde.string, Serde.string)
      .tap(e => Console.printLine(s"Got a record.... ${e.value}"))
      .mapZIO { record =>
        Console.printLine("mapZio...") *>
        ZIO
          .fromEither(zio.json.JsonDecoder[User].decodeJson(record.value))
          .mapError(err => new RuntimeException(s"Failed to decode user: $err"))
      }
      .tapError(err => Console.printLine(s"stream error ${err}"))
      .drain
  }

  def consume2: ZStream[Consumer, Throwable, User] = {
    println("Consume2")
    consumer
      .plainStream(Subscription.topics("user-events"), Serde.string, Serde.string)
      .tap(e => Console.printLine(s"Got a record.... ${e.value}"))
      .mapZIO { record =>
        Console.printLine("mapZio...") *>
          ZIO
            .fromEither(zio.json.JsonDecoder[User].decodeJson(record.value))
            .mapError(err => new RuntimeException(s"Failed to decode user: $err"))
      }
      .tapError(err => Console.printLine(s"stream error ${err}"))
  }
}

object KafkaIngressUserConsumerImpl {
  def layer: ZLayer[Consumer, Throwable, IngressUserConsumer] =
      ZLayer.fromFunction(KafkaIngressUserConsumerImpl.apply _).tap(x => Console.printLine(s"... ${x.toString()}"))
}
