package com.github.jedesah.thrift

import org.apache.thrift.protocol.{TStruct, TField, TList, TProtocol}
import shapeless._

package object serialization {
  def read[T](protocol: TProtocol)(implicit codec: ThriftCodec[T]): T = codec.read(protocol)
  def write[T](obj: T, protocol: TProtocol)(implicit codec: ThriftCodec[T]) = codec.write(obj, protocol)

  trait ThriftCodec[T] {
    def read(protocol: TProtocol): T
    def write(obj: T, protocol: TProtocol)
  }

  object ThriftCodec extends ProductTypeClassCompanion[ThriftCodec] {
    implicit val intCodec = new ThriftCodec[Int] {
      def read(protocol: TProtocol): Int = protocol.readI32()
      def write(obj: Int, protocol: TProtocol) { protocol.writeI32(obj) }
    }
    implicit val shortCodec = new ThriftCodec[Short] {
      def read(protocol: TProtocol): Short = protocol.readI16()
      def write(obj: Short, protocol: TProtocol) { protocol.writeI16(obj) }
    }
    implicit val doubleCodec = new ThriftCodec[Double] {
      def read(protocol: TProtocol): Double = protocol.readDouble()
      def write(obj: Double, protocol: TProtocol) { protocol.writeDouble(obj)}
    }
    implicit val booleanCodec = new ThriftCodec[Boolean] {
      def read(protocol: TProtocol): Boolean = protocol.readBool()
      def write(obj: Boolean, protocol: TProtocol) = { protocol.writeBool(obj)}
    }
    implicit val stringCodec = new ThriftCodec[String] {
      def read(protocol: TProtocol): String = protocol.readString()
      def write(obj: String, protocol: TProtocol) { protocol.writeString(obj)}
    }
    implicit def listCodec[T: ThriftCodec] = new ThriftCodec[List[T]] {
      def read(protocol: TProtocol): List[T] = {
        val listSpec = protocol.readListBegin()
        // TODO: Make sure the type of the list is correct
        val result: List[T] = (0 until listSpec.size).map(_ => serialization.read[T](protocol)).toList
        protocol.readListEnd()
        result
      }
      def write(obj: List[T], protocol: TProtocol): Unit = {
        // TODO: Add the type of the type of the list for validation
        val listSpec = new TList(0, obj.size)
        protocol.writeListBegin(listSpec)
        obj.foreach(serialization.write(_, protocol))
        protocol.writeListEnd()
      }
    }
    implicit def codecInstance: LabelledTypeClass[ThriftCodec] = new LabelledTypeClass[ThriftCodec] {
      def emptyProduct = new ThriftCodec[HNil] {
        def write(t: HNil, protocol: TProtocol) {} // No need to write anything to the protocol
        def read(protocol: TProtocol) = HNil // No need to read anything from the protocol
      }

      def product[F, T <: HList](name: String, FHead: ThriftCodec[F], FTail: ThriftCodec[T]) = new ThriftCodec[F :: T] {
        def write(ft: F :: T, protocol: TProtocol) {
          val fieldDescription = new TField(name, 0, -1)
          protocol.writeFieldBegin(fieldDescription)
          FHead.write(ft.head, protocol)
          protocol.writeFieldEnd()
          FTail.write(ft.tail, protocol)
        }
        def read(protocol: TProtocol): F :: T = {
          val fieldDescription = protocol.readFieldBegin()
          require(fieldDescription.name == name)
          val head = FHead.read(protocol)
          protocol.readFieldEnd()
          head :: FTail.read(protocol)
        }
      }

      def emptyCoproduct = new ThriftCodec[CNil] {
        def write(t: CNil, protocol: TProtocol) {}
        def read(protocol: TProtocol) {}
      }

      def coproduct[L, R <: Coproduct](name: String, CL: => ThriftCodec[L], CR: => ThriftCodec[R]) = new ThriftCodec[L :+: R] {
        def write(lr: L :+: R, protocol: TProtocol) = lr match {
          case Inl(l) => {
            val structDescription = new TStruct(name)
            protocol.writeStructBegin(structDescription)
            CL.write(l, protocol)
            protocol.writeFieldStop()
            protocol.writeStructEnd()
          }
        }
        def read(protocol: TProtocol): L :+: R = {
          val structDescription = protocol.readStructBegin()
          if (structDescription.name != name) throw new org.apache.thrift.transport.TTransportException()
          val result = CL.read(protocol)
          protocol.readStructEnd()
          Inl(result)
        }
      }

      def project[F, G](instance: => ThriftCodec[G], to : F => G, from : G => F) = new ThriftCodec[F] {
        def write(f: F, protocol: TProtocol) = instance.write(to(f), protocol)
        def read(protocol: TProtocol) = from(instance.read(protocol))
      }
    }
  }
}

