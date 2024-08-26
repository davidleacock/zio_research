package consumer

import domain.User
import zio._
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

case class KafkaUserConsumerImpl(consumer: Consumer) extends UserConsumer {

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

  // TODO do I need the additional explicit drain and offset commits? Does it happen by default?
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
object KafkaUserConsumerImpl {
  val layer: ZLayer[Consumer, Throwable, UserConsumer] = ZLayer.fromFunction(consumer => KafkaUserConsumerImpl(consumer))
}
