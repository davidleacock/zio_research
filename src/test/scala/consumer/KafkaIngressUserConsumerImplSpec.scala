package consumer

import com.dimafeng.testcontainers.KafkaContainer
import domain.User
import zio.{Scope, _}
import zio.kafka.consumer.Consumer.AutoOffsetStrategy
import zio.kafka.consumer.Consumer.OffsetRetrieval.Auto
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.test.Assertion._
import zio.test._

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
          .withOffsetRetrieval(Auto(AutoOffsetStrategy.Earliest))
          .withGroupId("test-group")
          .withClientId("test-client")
        consumer <- Consumer.make(consumerSettings)
      } yield consumer
    }

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("KafkaIngressUserConsumerImpl")(
      test("will consume valid data from user-event topic and create User") {
        ZIO.scoped {
          for {
            kafka <- ZIO.service[KafkaContainer]
            brokerUrl = kafka.bootstrapServers
            _ <- ZIO.succeed {
              val goodData = """{"id": "1", "name": "Alice"}"""
              publish(goodData, brokerUrl)
            }
            result <- KafkaIngressUserConsumerImpl
              .layer
              .build
              .flatMap { env =>
                env
                  .get[IngressUserConsumer]
                  .consume
                  .take(1)
                  .runCollect
              }
          } yield assertTrue(result == Chunk(User("1", "Alice")))
        }
      },
      test("will consume invalid data from user-event topic and return Error") {
        ZIO.scoped {
          for {
            kafka <- ZIO.service[KafkaContainer]
            brokerUrl = kafka.bootstrapServers
            _ <- ZIO.succeed {
              val badData = """{"bad": "data"}"""
              publish(badData, brokerUrl)
            }
            result <- KafkaIngressUserConsumerImpl
              .layer
              .build
              .flatMap { env =>
                env
                  .get[IngressUserConsumer]
                  .consume
                  .take(1)
                  .runCollect
                  .either
              }
          } yield assert(result)(
            isLeft(hasField("message", _.getMessage, containsString("Failed to decode user")))
          )
        }
      }
    ).provideLayer(
      kafkaContainerLayer >>> (consumerLayer ++ ZLayer.service[KafkaContainer])
    )

  private def publish(data: String, brokerUrl: String): Unit = {
    import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

    import java.util.Properties
    val props = new Properties()
    props.put("bootstrap.servers", brokerUrl)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](props)
    producer.send(new ProducerRecord[String, String]("user-events", "key", data))
    producer.close()
  }
}
