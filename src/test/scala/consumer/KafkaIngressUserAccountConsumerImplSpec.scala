package consumer

import com.dimafeng.testcontainers.KafkaContainer
import consumer.impl.KafkaIngressUserAccountConsumerImpl
import domain.UserAccount
import environment.TestInfrastructure
import zio._
import zio.test.Assertion._
import zio.test._

object KafkaIngressUserAccountConsumerImplSpec extends ZIOSpecDefault with TestInfrastructure {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("KafkaIngressUserAccountConsumerImpl")(
      test("will consume valid data from user-event topic and create UserAccount") {
        ZIO.scoped {
          for {
            kafka <- ZIO.service[KafkaContainer]
            brokerUrl = kafka.bootstrapServers
            _ <- ZIO.succeed {
              val goodData = """{"id": "1", "name": "Alice"}"""
              publish(goodData, brokerUrl)
            }
            result <- KafkaIngressUserAccountConsumerImpl
              .layer
              .build
              .flatMap { env =>
                env
                  .get[IngressUserAccountConsumer]
                  .consume
                  .take(1)
                  .runCollect
              }
          } yield assertTrue(result == Chunk(UserAccount("1", "Alice")))
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
            result <- KafkaIngressUserAccountConsumerImpl
              .layer
              .build
              .flatMap { env =>
                env
                  .get[IngressUserAccountConsumer]
                  .consume
                  .take(1)
                  .runCollect
                  .either
              }
          } yield assert(result)(
            isLeft(hasField("message", _.reason, containsString("Failed to decode user")))
          )
        }
      }
    ).provideLayer(
      kafkaContainerLayer >>> (consumerLayer ++ ZLayer.service[KafkaContainer])
    )
}
