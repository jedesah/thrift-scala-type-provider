package typeproviders.rdfs.public

import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.ThriftParser

import scala.annotation.StaticAnnotation
import scala.io.Source
import scala.language.experimental.macros
import scala.reflect.macros.Context

class fromSchema(path: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro PrefixGenerator.fromSchema_impl
}

object PrefixGenerator {

  def fromSchema_impl(c: Context)(annottees: c.Expr[Any]*) = {
    import c.universe._

    def bail(message: String) = c.abort(c.enclosingPosition, message)

    def thriftToScalaType(fieldType: FieldType): Tree = {
      fieldType match {
        case TBool => tq"Boolean"
        case TByte => tq"Byte"
        case TI16 => tq"Short"
        case TI32 => tq"Int"
        case TI64 => tq"Long"
        case TDouble => tq"Double"
        case TString => tq"String"
        case ReferenceType(refName) => Ident(TypeName(refName.fullName))
        case ListType(innerType, _) => tq"List[${thriftToScalaType(innerType)}]"
        case SetType(innerType, _) => tq"Set[${thriftToScalaType(innerType)}]"
      }
    }

    /** The expected usage will look something like this following:
      *
      * {{{
      * @fromSchema("/dcterms.rdf") object dc extends PrefixBuilder[Rdf] ...
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
        /** The following few steps look exactly like what we did in the case
          * of the anonymous type providers.
          */



        val defs = document.structs.map { struct =>

          val params = struct.fields.map { field =>
            val typeName = thriftToScalaType(field.fieldType)
            q"val ${TermName(field.originalName)}: $typeName"
          }
          println(params)

          q"case class ${TypeName(struct.originalName)}(..$params)"
        }

        //val defs = List(reify { case class Animal(age: Int, name: String) }.tree)

        /** We assume here that the parent is [[org.w3.banana.PrefixBuilder]].
          * We could add some validation logic to confirm this, but the macro
          * is likely to fail in a pretty straightforward way if it's not the
          * case, so we'll leave it like this for the sake of simplicity.
          */
        c.Expr[Any](
          q"""
            object $name {
              ..$defs
            }
          """
        )

      case _ => bail(
        "You must annotate an object definition with an empty body."
      )
    }
  }
}