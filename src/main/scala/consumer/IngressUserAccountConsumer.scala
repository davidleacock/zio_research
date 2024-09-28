package consumer

import domain.UserAccount
import zio.stream.ZStream

trait IngressUserAccountConsumer {
  def consume: ZStream[Any, IngressUserAccountError, UserAccount]
}

case class IngressUserAccountError(reason: String)

//// TODO? How do I think about this object method, how does it differ from the InMem repo?
//// Is there a pattern here?  Do I need this in the UserRepo trait?
object IngressUserAccountConsumer {
  def consume: ZStream[IngressUserAccountConsumer, IngressUserAccountError, UserAccount] =
    ZStream.serviceWithStream[IngressUserAccountConsumer](_.consume)
}
