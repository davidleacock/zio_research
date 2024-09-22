//package integration
//
//import com.dimafeng.testcontainers.KafkaContainer
//import domain.UserAccount
//import io.getquill.Literal
//import io.getquill.jdbczio.Quill
//import org.postgresql.ds.PGSimpleDataSource
//import org.testcontainers.containers.PostgreSQLContainer
//import org.testcontainers.utility.DockerImageName
//import processor.UserEventProcessor
//import repo.impl.{DataService, PostgresUserAccountRepositoryLive}
//import zio.http._
//import zio.kafka.consumer.Consumer.AutoOffsetStrategy
//import zio.kafka.consumer.Consumer.OffsetRetrieval.Auto
//import zio.kafka.consumer.{Consumer, ConsumerSettings}
//import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
//import zio.{Scope, ZIO, ZLayer, durationInt}
//
//import javax.sql.DataSource
//
//object UserAccountServiceSpec extends ZIOSpecDefault {
//
//  val kafkaContainerLayer: ZLayer[Any, Throwable, KafkaContainer] =
//    ZLayer.scoped {
//      ZIO.acquireRelease(ZIO.attempt {
//        val container = new KafkaContainer()
//        container.start()
//        container
//      })(container => ZIO.attempt(container.stop()).orDie)
//    }
//
//  val consumerLayer: ZLayer[KafkaContainer with Scope, Throwable, Consumer] =
//    ZLayer.fromZIO {
//      for {
//        kafka <- ZIO.service[KafkaContainer]
//        brokerUrl = kafka.bootstrapServers
//        consumerSettings = ConsumerSettings(List(brokerUrl))
//          .withOffsetRetrieval(Auto(AutoOffsetStrategy.Earliest))
//          .withGroupId("test-group")
//          .withClientId("test-client")
//        consumer <- Consumer.make(consumerSettings)
//      } yield consumer
//    }
//
//  val postgresContainer: ZLayer[Any, Throwable, PostgreSQLContainer[Nothing]] =
//    ZLayer.scoped {
//      ZIO.acquireRelease {
//        ZIO.attempt {
//          val container = new PostgreSQLContainer[Nothing](DockerImageName.parse("postgres:15.3"))
//          container.start()
//          container
//        }
//      }(container => ZIO.succeed(container.stop()))
//    }
//
//  val dataSourceLive: ZLayer[PostgreSQLContainer[Nothing], Throwable, DataSource] =
//    ZLayer.fromZIO {
//      ZIO.serviceWith[PostgreSQLContainer[Nothing]] { container =>
//        val dataSource = new PGSimpleDataSource()
//        dataSource.setURL(container.getJdbcUrl)
//        dataSource.setUser(container.getUsername)
//        dataSource.setPassword(container.getPassword)
//        dataSource
//      }
//    }
//
//  val postgresLive: ZLayer[DataSource, Nothing, Quill.Postgres[Literal]] =
//    Quill.Postgres.fromNamingStrategy(Literal)
//
//  val dataServiceLive: ZLayer[Quill.Postgres[Literal], Nothing, DataService] = ZLayer.fromFunction(DataService.apply _)
//
//  val userRepositoryLayer: ZLayer[Quill.Postgres[Literal], Nothing, PostgresUserAccountRepositoryLive] =
//    dataServiceLive >>> ZLayer.fromFunction(PostgresUserAccountRepositoryLive.apply _)
//
//  val applicationLayer: ZLayer[TestEnvironment with Scope, Throwable, PostgresUserAccountRepositoryLive with Quill.Postgres[Literal] with Consumer with KafkaContainer with Server] =
//    (postgresContainer >>> dataSourceLive >>> postgresLive >>> (userRepositoryLayer ++ ZLayer
//      .service[Quill.Postgres[Literal]])) ++
//      (kafkaContainerLayer >>> (consumerLayer ++ ZLayer.service[KafkaContainer])) ++
//      Server.default
//
//  override def spec: Spec[TestEnvironment with Scope, Any] =
//    suite("UserAccountService")(
//      test("will consumer user events from kafka and write them to a database that can be queried via http") {
//
//        val userAccount = UserAccount("1", "Name")
//
//        val publishToKafka = for {
//          kafka <- ZIO.service[KafkaContainer]
//          brokerUrl = kafka.bootstrapServers
//          _ <- ZIO.succeed {
//            val goodData = """{"id": "1", "name": "Alice"}"""
//            publish(goodData, brokerUrl)
//          }
//        } yield ()
//
//        val consumeAndProcess = for {
//          eventProcessing <- UserEventProcessor.processStream.fork
//          _ <- publishToKafka
//          _ <- ZIO.sleep(5.seconds) // Can this be replaced with something?
//          _ <- eventProcessing.interrupt
//        } yield ()
//
//        val url = URL.decode("http://localhost:8080/users/1").toOption.get
//        val testHttp = for {
//          data <- ZClient.batched(Request.get(url))
//          body <- data.body.asString
//        } yield assertTrue(data.status == Status.Ok && body.contains("asdasd"))
//
//        (consumeAndProcess zipPar testHttp)
//      }
//    ).provideLayerShared(applicationLayer)
//
//  private def publish(data: String, brokerUrl: String): Unit = {
//    import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
//
//    import java.util.Properties
//    val props = new Properties()
//    props.put("bootstrap.servers", brokerUrl)
//    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
//    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
//
//    val producer = new KafkaProducer[String, String](props)
//    producer.send(new ProducerRecord[String, String]("user-events", "key", data))
//    producer.close()
//  }
//}
