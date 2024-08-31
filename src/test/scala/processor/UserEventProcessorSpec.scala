package processor

import consumer.IngressUserConsumer
import domain.User
import domain.UserError.PersistenceError
import repo.UserRepository
import zio._
import zio.kafka.consumer.Consumer
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._

object UserEventProcessorSpec extends ZIOSpecDefault {

  class MockIngressUserConsumer(users: List[User]) extends IngressUserConsumer {
    override def consume: ZStream[Any, Throwable, User] = ZStream.fromIterable(users)

    override def consume2: ZStream[Consumer, Throwable, User] = ZStream.fromIterable(users)
  }

  // TODO fix InMemoryRepo so I can use that, fix constructor I think?
  // TODO If I want to use InMemory then I need to pass in a ref, rather than a map
  class MockUserRepository(ref: Ref[List[User]]) extends UserRepository {
    def create(user: User): ZIO[Any, Throwable, Unit] =
      if (user.name.isBlank || user.id.isBlank) {
        ZIO.fail(new RuntimeException("Name and/or Id is blank."))
      } else ref.update(_ :+ user)
    def get(id: String): ZIO[Any, Throwable, Option[User]] = ref.get.map(_.find(_.id == id))
    def list(): ZIO[Any, Throwable, List[User]] = ref.get
  }

  val data: List[User] = List(User("1", "Alice"), User("2", "Bob"))

  val badData: List[User] = List(User("1", ""))

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserEventProcessorSpec")(
      test("Processor stream consumes valid Users and persists in repo.") {
        for {
          ref <- Ref.make(List.empty[User])
          repo = new MockUserRepository(ref)
          consumers = new MockIngressUserConsumer(data)
          _ <- UserEventProcessor
            .processStream
            .provideLayer(ZLayer.succeed(consumers) ++ ZLayer.succeed(repo))
          users <- ref.get
        } yield assertTrue(users == data)
      },
      test("Processor stream handles invalid Users with error.") {
        for {
          ref <- Ref.make(List.empty[User])
          repo = new MockUserRepository(ref)
          consumers = new MockIngressUserConsumer(badData)
          result <- UserEventProcessor
            .processStream
            .provideLayer(ZLayer.succeed(consumers) ++ ZLayer.succeed(repo))
            .either
        } yield assert(result)(isLeft(equalTo(PersistenceError("Name and/or Id is blank."))))
      }
    )
}
