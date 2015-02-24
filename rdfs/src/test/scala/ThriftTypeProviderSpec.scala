import org.specs2.mutable._

import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

class ThriftTypeProviderSpec extends Specification {

  "The @fromSchema" should {
    "compile" in {
      "simple" in {
        val tb = runtimeMirror(getClass.getClassLoader).mkToolBox()
        val AST = tb.parse(
          """
            |import typeproviders.rdfs.public._
            |
            |  @fromSchema("/example.thrift") object dct
            |
            |  val a = dct.Point(4)
            |  val b = dct.Elem(3)
            |  val c = dct.Missing(true,"allo", dct.Elem(4))
            |  println(a)
            |  println(b)
            |  println(a.x)
          """.stripMargin)
        tb.typecheck(AST)
        ok
      }
    }
  }
}