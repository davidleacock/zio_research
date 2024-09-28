package integration

import com.dimafeng.testcontainers.KafkaContainer
import domain.UserAccount
import processor.UserEventProcessorStream
import repo.impl.PostgresUserAccountRepositoryLive
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}

object UserEventProcessorStreamSpec extends ZIOSpecDefault with TestInfrastructure {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserEventProcessor")(
      test("processor stream consumes valid UserAccount from Kafka and persists in Postgres repo") {
        val actualUser = """{"id": "1", "name": "Alice"}"""
        val expectedUser = UserAccount("1", "Alice")

        val publishToKafka = for {
          kafka <- ZIO.service[KafkaContainer]
          brokerUrl = kafka.bootstrapServers
          _ <- ZIO.succeed {
            publish(actualUser, brokerUrl)
          }
        } yield ()

        for {
          repo <- ZIO.service[PostgresUserAccountRepositoryLive]
          _ <- schemaInitialization
          eventProcessingFibre <- UserEventProcessorStream
            .processStream
            .take(1)
            .runCollect
            .fork
          _ <- publishToKafka
          _ <- eventProcessingFibre.join
          users <- repo.list()
        } yield assertTrue(users.contains(expectedUser))
      }
    ).provideLayer(kafkaLayer ++ repoLayer)
}
