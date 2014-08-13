package com.github.jedesah.thrift

import org.specs2.mutable._

import scala.reflect.runtime.universe._

class ThriftTypeProviderSpec extends Specification {

  "generateScalaAST" should {
    "simple" in {
      val thriftDefinition =
        """
          |struct Point {
          |    1: double x
          |}
        """.stripMargin
      val actual = Thrift.generateScalaASTs(scala.reflect.runtime.universe, thriftDefinition)
      val expected = q"case class Point(x: Double)"
      actual must have length(1)
      if (actual.head equalsStructure expected) ok else actual.head ==== expected
    }
  }
}
