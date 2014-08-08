package typeproviders.rdfs.examples


object MyApp extends App {

  import typeproviders.rdfs.public._

  @fromSchema("/dctype.rdf") object dct

  import dct._

  val a = Point(4)
  val b = Elem(3)
  val c = Missing(true,List("allo"), Elem(4), Set(true), Map("hey" -> "you"))
  println(a)
  println(b)
  println(a.x)

  object MyImpl extends Heartbeet {
    def ping(greet: String, c: Thing): String = "greetings!"
  }

  val h: Heartbeet = MyImpl
  h.ping("hello", Thing.a(Point(4)))
}