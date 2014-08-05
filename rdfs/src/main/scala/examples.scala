package typeproviders.rdfs.examples


object MyApp extends App {

  import typeproviders.rdfs.public._

  @fromSchema("/dctype.rdf") object dct

  val a = dct.Point(4)
  val b = dct.Elem(3)
  val c = dct.Missing(true,List("allo"), dct.Elem(4), Set(true), Map("hey" -> "you"))
  println(a)
  println(b)
  println(a.x)
}