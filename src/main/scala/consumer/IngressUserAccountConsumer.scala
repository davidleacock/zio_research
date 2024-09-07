package consumer

import domain.UserAccount
import zio.stream.ZStream

trait IngressUserAccountConsumer {
  def consume: ZStream[Any, Throwable, UserAccount]
}

//// TODO? How do I think about this object method, how does it differ from the InMem repo?
//// Is there a pattern here?  Do I need this in the UserRepo trait?
object IngressUserAccountConsumer {
  def consume: ZStream[IngressUserAccountConsumer, Throwable, UserAccount] =
    ZStream.serviceWithStream[IngressUserAccountConsumer](_.consume)
}
