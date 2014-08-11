package com.github.jedesah.thrift

object Example extends App {

  @Thrift("/example.thrift") object Thrift

  import Thrift._

  val a = Point(4)
  val b = Elem(3)
  val c = Missing(aa = List("allo"), i = Elem(4), bb = Set(true), cc = Map("hey" -> "you"))
  println(a)
  println(b)
  println(a.x)

  object MyImpl extends Heartbeet {
    def ping(greet: String, c: Thing): String = "greetings!"
  }

  val h: Heartbeet = MyImpl
  h.ping("hello", Thing.a(Point(4)), Day.Monday)
}