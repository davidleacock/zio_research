package processor

import consumer.IngressUserAccountConsumer
import domain.UserAccount
import domain.UserError.PersistenceError
import repo.UserAccountRepository
import zio._
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._

object UserAccountEventProcessorSpec extends ZIOSpecDefault {

  class MockIngressUserAccountConsumer(users: List[UserAccount]) extends IngressUserAccountConsumer {
    override def consume: ZStream[Any, Throwable, UserAccount] = ZStream.fromIterable(users)
  }

  // TODO Use InMemoryRepo rather than rewriting this here
  class MockUserAccountRepository(ref: Ref[List[UserAccount]]) extends UserAccountRepository {
    def create(user: UserAccount): ZIO[Any, Throwable, Unit] =
      if (user.name.isBlank || user.id.isBlank) {
        ZIO.fail(new RuntimeException("Name and/or Id is blank."))
      } else ref.update(_ :+ user)
    def get(id: String): ZIO[Any, Throwable, Option[UserAccount]] = ref.get.map(_.find(_.id == id))
    def list(): ZIO[Any, Throwable, List[UserAccount]] = ref.get
  }

  val data: List[UserAccount] = List(UserAccount("1", "Alice"), UserAccount("2", "Bob"))

  val badData: List[UserAccount] = List(UserAccount("1", ""))

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserEventProcessorSpec")(
      test("Processor stream consumes valid Users and persists in repo.") {
        for {
          ref <- Ref.make(List.empty[UserAccount])
          repo = new MockUserAccountRepository(ref)
          consumers = new MockIngressUserAccountConsumer(data)
          _ <- UserEventProcessorStream
            .processStream
            .take(2)
            .runCollect
            .provideLayer(ZLayer.succeed(consumers) ++ ZLayer.succeed(repo))
          users <- ref.get
        } yield assertTrue(users == data)
      },
      test("Processor stream handles invalid Users with error.") {
        for {
          ref <- Ref.make(List.empty[UserAccount])
          repo = new MockUserAccountRepository(ref)
          consumers = new MockIngressUserAccountConsumer(badData)
          result <- UserEventProcessorStream
            .processStream
            .take(1)
            .runCollect
            .provideLayer(ZLayer.succeed(consumers) ++ ZLayer.succeed(repo))
            .either
        } yield assert(result)(isLeft(equalTo(EventProcessorFail("Error persisting to user account repo: Name and/or Id is blank."))))
      }
    )
}
