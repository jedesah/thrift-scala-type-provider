package com.github.jedesah.thrift

import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.ThriftParser

import scala.annotation.StaticAnnotation
import scala.io.Source
import scala.language.experimental.macros
import scala.reflect.macros.Context

class Thrift(path: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Thrift.fromSchema_impl
}

object Thrift {

  def fromSchema_impl(c: Context)(annottees: c.Expr[Any]*) = {
    import c.universe._

    def bail(message: String) = c.abort(c.enclosingPosition, message)

    implicit object LiftFieldType extends Liftable[FieldType] {
      def apply(value: FieldType): Tree = value match {
        case TBool => tq"Boolean"
        case TByte => tq"Byte"
        case TI16 => tq"Short"
        case TI32 => tq"Int"
        case TI64 => tq"Long"
        case TDouble => tq"Double"
        case TString => tq"String"
        case ReferenceType(refName) => Ident(TypeName(refName.fullName))
        case ListType(elemType, _) => tq"List[$elemType]"
        case SetType(elemType, _) => tq"Set[$elemType]"
        case MapType(keyType, valueType, _) => tq"Map[$keyType, $valueType]"
      }
    }

    implicit val liftFunType: Liftable[FunctionType] = Liftable[FunctionType] {
      case TBool => tq"Boolean"
      case TByte => tq"Byte"
      case TI16 => tq"Short"
      case TI32 => tq"Int"
      case TI64 => tq"Long"
      case TDouble => tq"Double"
      case TString => tq"String"
      case ReferenceType(refName) => Ident(TypeName(refName.fullName))
      case ListType(elemType, _) => tq"List[$elemType]"
      case SetType(elemType, _) => tq"Set[$elemType]"
      case MapType(keyType, valueType, _) => tq"Map[$keyType, $valueType]"
    }

    /** The expected usage will look something like this following:
      *
      * {{{
      * @Thrift("/service_definition.thrift") object calculator
      * }}}
      *
      * The argument to the annotation must be a string literal, since we need
      * to know its value at compile-time (i.e., now) in order to read and
      * parse the schema. The following code digs into the tree of the macro
      * application and confirms that we have a string literal.
      */
    val filename = c.macroApplication match {
      case Apply(Select(Apply(_, List(Literal(Constant(s: String)))), _), _) =>
        s
      case _ => bail(
        "You must provide a literal resource path for schema parsing."
      ) 
    }

    val stream = this.getClass.getResourceAsStream(filename)
    val contents = Source.fromInputStream(stream).getLines().mkString("\n")
    val parser = new ThriftParser(true)

    val document = parser.parse(contents, parser.document)

    annottees.map(_.tree) match {
      /** Note that we're checking that the body of the annotated object is
        * empty, since in this case it wouldn't make sense for the user to add
        * his or her own methods to the prefix object. For other kinds of type
        * providers this might be reasonable or even desirable. In these cases
        * you'd simply remove the check for emptiness below and add the body
        * to the definition you return.
        */
      case List(q"object $name extends $parent { ..$body }") if body.isEmpty =>

        val unions = document.defs.collect { case u:Union => u }

        val unionsScala = unions.map { union =>
            val children = union.fields.map { field =>
              q"case class ${TypeName(field.originalName)}(x: ${field.fieldType}) extends ${TypeName(union.originalName)}"
            }
            val unionType = q"trait ${TypeName(union.originalName)}"
            val companion = q"object ${TermName(union.originalName)} {..$children}"
            val topLevel = Seq(unionType, companion)
            topLevel
        }

        val structs = document.defs.collect { case s: Struct =>s }
        val structsAsScalaCaseClass = structs.map { struct =>

          val params = struct.fields.map { field =>
            q"val ${TermName(field.originalName)}: ${field.fieldType}"
          }

          q"case class ${TypeName(struct.originalName)}(..$params)"
        }

        val services = document.services.map { service =>
          val functions = service.functions.map { fun =>
            val params = fun.args.map { arg =>
              q"val ${TermName(arg.originalName)}: ${arg.fieldType}"
            }
            q"def ${TermName(fun.originalName)}(..$params): ${fun.funcType}"
          }
          val name = service.sid.name
          val interface = q"trait ${TypeName(name)} { ..$functions }"
          val functionImpl = functions.map { case DefDef(mods, name, typeDefs, paramss, tpe, _) =>
            val nameString = name.toString
            val params = paramss.flatten
            val methodNameLiteral = {Literal(Constant(nameString))}
            val argsName = name + "$args"
            val argsStruct = q"case class ${TypeName(argsName)}(..${params})"
            val impl = q"send($methodNameLiteral, ${TermName(argsName)}(..${params.map(_.name)}));receive($methodNameLiteral);"
            val funWithImpl = DefDef(NoMods, name, typeDefs, paramss, tpe, impl)

            Seq(argsStruct, funWithImpl)
          }
          val companion = q"""
              object ${TermName(name)} {
                import org.apache.thrift.protocol.TProtocol
                import com.github.jedesah.thrift.{Client => BaseClient}
                case class Client(protocol: TProtocol) extends BaseClient(protocol) with ${TypeName(name)} {
                  ..${functionImpl.flatten}
                }
              }
          """
          List(interface, companion)
        }

        val defs = unionsScala.flatten ++ structsAsScalaCaseClass ++ services.flatten
        val result = c.Expr[Any](
          q"""
            object $name {
              ..$defs
            }
          """
        )
        println(showCode(result.tree))
        result

      case _ => bail(
        "You must annotate an object definition with an empty body."
      )
    }
  }
}