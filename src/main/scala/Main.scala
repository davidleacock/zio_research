import consumer.IngressUserAccountConsumer
import consumer.impl.KafkaIngressUserAccountConsumerImpl
import domain.UserAccount
import io.getquill.{Literal, Update}
import io.getquill.jdbczio.Quill
import org.postgresql.ds.PGSimpleDataSource
import processor.{EventProcessorFail, UserEventProcessorStream}
import repo.UserAccountRepository
import repo.impl.{DataService, PostgresUserAccountRepositoryLive}
import zio._
import http.Server
import server.UserAccountServer
import zio.kafka.consumer.Consumer.AutoOffsetStrategy
import zio.kafka.consumer.Consumer.OffsetRetrieval.Auto
import zio.kafka.consumer.{Consumer, ConsumerSettings}

import javax.sql.DataSource

object Main extends ZIOAppDefault {


  val dataSourceLayer: ZLayer[Any, Throwable, DataSource] = ZLayer.fromZIO {
    ZIO.attempt {
      val dataSource = new PGSimpleDataSource()
      dataSource.setURL("jdbc:postgresql://localhost:5432/test_db")
      dataSource.setUser("user")
      dataSource.setPassword("password")
      dataSource
    }
  }

  val postgresLive: ZLayer[DataSource, Nothing, Quill.Postgres[Literal]] =
    Quill.Postgres.fromNamingStrategy(Literal)

  val dataServiceLive: ZLayer[Quill.Postgres[Literal], Nothing, DataService] =
    ZLayer.fromFunction(DataService.apply _)

  val quillLayer: ZLayer[Any, Throwable, Quill.Postgres[Literal]] =
    dataSourceLayer >>> postgresLive

  val userRepositoryLayer: ZLayer[Any, Throwable, UserAccountRepository] =
    (quillLayer >>> dataServiceLive) >>>
      ZLayer.fromFunction(PostgresUserAccountRepositoryLive.apply _)

  val consumerLayer: ZLayer[Any, Throwable, Consumer] = ZLayer.scoped {
    val consumerSettings = ConsumerSettings(List("localhost:9092"))
      .withOffsetRetrieval(Auto(AutoOffsetStrategy.Earliest))
      .withGroupId("application-group")
      .withClientId("application-client")
    for {
       consumer <- Consumer.make(consumerSettings)
    } yield consumer
  }

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

  val ingressConsumerLayer: ZLayer[Any, Throwable, IngressUserAccountConsumer] =
    consumerLayer >>> KafkaIngressUserAccountConsumerImpl.layer

  val serverLayer: ZLayer[Any, Throwable, Server] = Server.default

  val eventProcessor: ZIO[IngressUserAccountConsumer with UserAccountRepository, EventProcessorFail, Unit] =
    UserEventProcessorStream.processStream.runDrain

  val serverApp: ZIO[UserAccountRepository with Server, Throwable, Nothing] =
    UserAccountServer.server


  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val layers = ingressConsumerLayer ++ userRepositoryLayer ++ serverLayer ++ quillLayer

    val program = for {
      _ <- schemaInitialization
      _ <- eventProcessor.forkDaemon
      _ <- serverApp
    } yield ()

    program.provideLayer(layers)
  }
}
