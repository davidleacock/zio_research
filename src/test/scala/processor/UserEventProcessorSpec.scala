package processor

import consumer.UserConsumer
import domain.User
import repo.UserRepository
import zio._
import zio.stream.ZStream
import zio.test._

object UserEventProcessorSpec extends ZIOSpecDefault {

  class MockUserConsumer(users: List[User]) extends UserConsumer {
    override def consume: ZStream[Any, Throwable, User] = ZStream.fromIterable(users)
  }

  class MockUserRepository(ref: Ref[List[User]]) extends UserRepository {
    def create(user: User): ZIO[Any, Throwable, Unit] = ref.update(_ :+ user)
    def get(id: String): ZIO[Any, Throwable, Option[User]] = ref.get.map(_.find(_.id == id))
    def list(): ZIO[Any, Throwable, List[User]] = ref.get
  }

  val data: List[User] = List(User("1", "Alice"), User("2", "Bob"))

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserEventProcessorSpec")(
      test("Processor stream consumes Users and persists in repo.") {
        for {
          ref <- Ref.make(List.empty[User])
          repo = new MockUserRepository(ref)
          consumers = new MockUserConsumer(data)
          _ <- UserEventProcessor.processStream.provideLayer(ZLayer.succeed(consumers) ++ ZLayer.succeed(repo))
          users <- ref.get
        } yield assertTrue(users == data)
      }
    )
}