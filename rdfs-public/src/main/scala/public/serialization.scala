package com.github.jedesah.thrift

import org.apache.thrift.protocol.TProtocol
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
    implicit def codecInstance: ProductTypeClass[ThriftCodec] = new ProductTypeClass[ThriftCodec] {
      def emptyProduct = new ThriftCodec[HNil] {
        def write(t: HNil, protocol: TProtocol) = {} // No need to write anything to the protocol
        def read(protocol: TProtocol) = HNil // No need to read anything from the protocol
      }

      def product[F, T <: HList](FHead: ThriftCodec[F], FTail: ThriftCodec[T]) = new ThriftCodec[F :: T] {
        def write(ft: F :: T, protocol: TProtocol) = {
          FHead.write(ft.head, protocol)
          FTail.write(ft.tail, protocol)
        }
        def read(protocol: TProtocol): F :: T = {
          FHead.read(protocol) :: FTail.read(protocol)
        }
      }

      def project[F, G](instance: => ThriftCodec[G], to : F => G, from : G => F) = new ThriftCodec[F] {
        def write(f: F, protocol: TProtocol) = instance.write(to(f), protocol)
        def read(protocol: TProtocol) = from(instance.read(protocol))
      }
    }
  }
}

