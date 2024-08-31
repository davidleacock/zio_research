package consumer

import com.dimafeng.testcontainers.KafkaContainer
import domain.User
import org.apache.kafka.clients.producer.RecordMetadata
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio._
import zio.kafka.consumer.Consumer.AutoOffsetStrategy
import zio.kafka.consumer.Consumer.OffsetRetrieval.Auto
import zio.test._
import zio.{Scope, _}

object KafkaIngressUserConsumerImplSpec extends ZIOSpecDefault {

  val kafkaContainerLayer: ZLayer[Any, Throwable, KafkaContainer] =
    ZLayer.scoped {
      ZIO.acquireRelease(ZIO.attempt {
        val container = new KafkaContainer()
        container.start()
        container
      })(container => ZIO.attempt(container.stop()).orDie)
    }

  val consumerLayer: ZLayer[KafkaContainer with Scope, Throwable, Consumer] =
    ZLayer.fromZIO {
      for {
        kafka <- ZIO.service[KafkaContainer]
        brokerUrl = kafka.bootstrapServers
        consumerSettings = ConsumerSettings(List(brokerUrl))
          .withOffsetRetrieval(Auto(AutoOffsetStrategy. Earliest))
          .withGroupId("test-group-1")
          .withClientId("test-client-1")
        consumer <- Consumer.make(consumerSettings).tap(x => Console.printLine(s"consumerLayer: ${x.toString}"))
      } yield consumer
    }

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("KafkaIngressUserConsumerImpl")(
      test("Ingress consumer will consume user-event data") {
        ZIO.scoped {
          for {
            kafka <- ZIO.service[KafkaContainer]

            brokerUrl = kafka.bootstrapServers
            consumerSettings = ConsumerSettings(List(brokerUrl))
              .withGroupId("test-group")
              .withClientId("test-client")

            _ <- ZIO.succeed {
              import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

              import java.util.Properties
              val props = new Properties()
              props.put("bootstrap.servers", brokerUrl)
              props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
              props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

              val producer = new KafkaProducer[String, String](props)
              producer.send(
                new ProducerRecord[String, String]("user-events", "key", """{"id": "1", "name": "Alice"}"""),
                (metadata: RecordMetadata, exception: Exception) =>
                  if (exception != null) {
                    println(s"Error sending message ${exception.getMessage}")
                  } else {
                    println(s"Message sent to topic ${metadata.topic()}, partition ${metadata.partition()}, offset ${metadata.offset()}")
                  }
              )
              producer.close()
            }

            result <- KafkaIngressUserConsumerImpl
              .layer
              .build
              .flatMap { env =>


                Console.printLine("Starting IngressUserConsumer") *>
                  env
                    .get[IngressUserConsumer]
                    .consume2
                    .tap(x => Console.printLine(s"got ${x.name}"))
                    .take(1)
                    .tap(x => Console.printLine(s"got ${x.name}"))
                    .runCollect

              }
              .tapBoth(
                err => Console.printLine(s"Error in stream ${err.getMessage}"),
                result => Console.printLine(s"Stream result $result")
              )
          } yield assertTrue(result == Chunk(User("1", "Alice")))
        }
      }.provideLayer(
        kafkaContainerLayer >>> (consumerLayer ++ ZLayer.service[KafkaContainer])
      )
    )
}
