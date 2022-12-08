package it.unibo

package object script {
  class FunctionWrapper[S, A](function: S => A) extends (S => A) {
    override def apply(v1: S): A = function.apply(v1)
  }
  implicit class Unsafe(elem: Any) {
    def as[A]: A = elem.asInstanceOf[A]
  }
  implicit def unsane[A](obj: Object): A = obj.asInstanceOf[A]

  implicit class LambdaWrapper[S, A](f: S => A) {
    def lift(): FunctionWrapper[S, A] = new FunctionWrapper[S, A](f)
  }
}
