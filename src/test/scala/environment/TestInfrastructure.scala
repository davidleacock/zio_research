package environment

import com.dimafeng.testcontainers.KafkaContainer
import consumer.IngressUserAccountConsumer
import consumer.impl.KafkaIngressUserAccountConsumerImpl
import domain.UserAccount
import io.getquill.jdbczio.Quill
import io.getquill.{Literal, Update}
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import repo.impl.{DataService, PostgresUserAccountRepositoryLive}
import zio.kafka.consumer.Consumer.AutoOffsetStrategy
import zio.kafka.consumer.Consumer.OffsetRetrieval.Auto
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.test.TestEnvironment
import zio.{Scope, ZIO, ZLayer}

import javax.sql.DataSource

trait TestInfrastructure {

  val postgresContainer: ZLayer[Any, Throwable, PostgreSQLContainer[Nothing]] =
    ZLayer.scoped {
      ZIO.acquireRelease {
        ZIO.attempt {
          val container = new PostgreSQLContainer[Nothing](DockerImageName.parse("postgres:15.3"))
          container.start()
          container
        }
      }(container => ZIO.succeed(container.stop()))
    }

  val dataSourceLive: ZLayer[PostgreSQLContainer[Nothing], Throwable, DataSource] =
    ZLayer.fromZIO {
      ZIO.serviceWith[PostgreSQLContainer[Nothing]] { container =>
        val dataSource = new PGSimpleDataSource()
        dataSource.setURL(container.getJdbcUrl)
        dataSource.setUser(container.getUsername)
        dataSource.setPassword(container.getPassword)
        dataSource
      }
    }

  val postgresLive: ZLayer[DataSource, Nothing, Quill.Postgres[Literal]] =
    Quill.Postgres.fromNamingStrategy(Literal)

  val dataServiceLive: ZLayer[Quill.Postgres[Literal], Nothing, DataService] =
    ZLayer.fromFunction(DataService.apply _)

  val userRepositoryLayer: ZLayer[Quill.Postgres[Literal], Nothing, PostgresUserAccountRepositoryLive] =
    dataServiceLive >>> ZLayer.fromFunction(PostgresUserAccountRepositoryLive.apply _)

  val schemaInitialization: ZIO[Quill.Postgres[Literal], Throwable, Unit] =
    ZIO.serviceWithZIO[Quill.Postgres[Literal]] { ctx =>
      import ctx._
      val createSchema = quote {
        sql"""
          CREATE TABLE IF NOT EXISTS "useraccount" (
            id VARCHAR(255) PRIMARY KEY,
            name VARCHAR(255) NOT NULL
          )
        """.as[Update[UserAccount]]
      }
      ctx.run(createSchema).unit
    }

  val repoLayer
  : ZLayer[TestEnvironment with Scope, Throwable, PostgresUserAccountRepositoryLive with Quill.Postgres[Literal]] =
    postgresContainer >>>
      dataSourceLive >>>
      postgresLive >>>
      (userRepositoryLayer ++ ZLayer.service[Quill.Postgres[Literal]])

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

  val kafkaConsumerImplLayer: ZLayer[Consumer, Throwable, IngressUserAccountConsumer] =
    KafkaIngressUserAccountConsumerImpl.layer

  val kafkaLayer: ZLayer[TestEnvironment with Scope, Throwable, IngressUserAccountConsumer with KafkaContainer] =
    kafkaContainerLayer >>>
      (consumerLayer >>> kafkaConsumerImplLayer ++ ZLayer.service[KafkaContainer])


  def publish(data: String, brokerUrl: String): Unit = {
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
