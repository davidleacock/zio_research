package consumer

import domain.User
import zio.stream.ZStream

trait UserConsumer {
  def consume: ZStream[Any, Throwable, User]
}

// TODO? How do I think about this object method, how does it differ from the InMem repo?
// Is there a pattern here?  Do I need this in the UserRepo trait?
object UserConsumer {
  def consume: ZStream[UserConsumer, Throwable, User] = ZStream.serviceWithStream[UserConsumer](_.consume)
}
