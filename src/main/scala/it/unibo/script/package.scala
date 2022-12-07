package it.unibo

package object script {
  implicit class Unsafe(elem: Any) {
    def as[A]: A = elem.asInstanceOf[A]
  }
  implicit def unsane[A](obj: Object): A = obj.asInstanceOf[A]
}
