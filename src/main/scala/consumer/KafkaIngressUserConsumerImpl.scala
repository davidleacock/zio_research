package consumer

import domain.User
import zio._
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

case class KafkaIngressUserConsumerImpl(consumer: Consumer) extends IngressUserConsumer {

  // TODO - Currently this will just log whatever we consume, but we need to do something with the User
//  override def consume: ZStream[Any, Throwable, User] = {
//    consumer
//      .plainStream(Subscription.topics("not-yet-made"), Serde.string, Serde.string)
//      .tap(record => Console.printLine(s"Receiving $record via kafka"))
//      .map(record => record.offset)
//      .aggregateAsync(Consumer.offsetBatches) // What does any of this do?
//      .mapZIO(offsetBatch => offsetBatch.commit)
//      .drain
//  }

  // TODO need to test this with a Kafka Container

  // TODO do I need the additional explicit drain and offset commits? Does it happen by default?
  // TODO how do I pass in the topic information?
  override def consume: ZStream[Any, Throwable, User] = {
    consumer
      .plainStream(Subscription.topics("not-yet-made"), Serde.string, Serde.string)
      .mapZIO {  record =>
        ZIO.fromEither(zio.json.JsonDecoder[User].decodeJson(record.value))
          .mapError(err => new RuntimeException(s"Failed to decode user: $err"))
      }
  }
}



// Is this the best way to build this?
// We still need the Consumer layer
/*
 def consumerLayer =
    ZLayer.scoped(
      Consumer.make(
        ConsumerSettings(List("localhost:29092")).withGroupId("group")
      )
    )
 */
object KafkaIngressUserConsumerImpl {
  def layer(settings: ConsumerSettings): ZLayer[Consumer, Throwable, IngressUserConsumer] = {
    // TODO wtf is Scoped?
    val consumerLayer = ZLayer.scoped(Consumer.make(settings))
    consumerLayer >>> ZLayer.fromFunction(consumer => KafkaIngressUserConsumerImpl(consumer))
  }
}
