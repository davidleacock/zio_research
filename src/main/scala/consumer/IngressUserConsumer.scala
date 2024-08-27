package consumer

import domain.User
import zio.stream.ZStream

trait IngressUserConsumer {
  def consume: ZStream[Any, Throwable, User]
}

// TODO? How do I think about this object method, how does it differ from the InMem repo?
// Is there a pattern here?  Do I need this in the UserRepo trait?
object IngressUserConsumer {
  def consume: ZStream[IngressUserConsumer, Throwable, User] = ZStream.serviceWithStream[IngressUserConsumer](_.consume)
}
