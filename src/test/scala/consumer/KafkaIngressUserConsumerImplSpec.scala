package consumer

import com.dimafeng.testcontainers.KafkaContainer
import domain.User
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio._
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
          .withGroupId("test-group")
          .withClientId("test-client")
        consumer <- Consumer.make(consumerSettings)
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
              producer.send(new ProducerRecord[String, String]("user-events", "key", """{"id": "1", "name": "Alice"}"""))
              producer.close()
            }

            result <- KafkaIngressUserConsumerImpl
              .layer(consumerSettings)
              .build
              .flatMap { env =>
                env.get[IngressUserConsumer].consume.take(1).runCollect
              }
          } yield assertTrue(result == Chunk(User("1", "Aasdasdlice")))
        }
      }.provideLayerShared(
        kafkaContainerLayer >>> (consumerLayer ++ ZLayer.service[KafkaContainer])
      )
    )
}
