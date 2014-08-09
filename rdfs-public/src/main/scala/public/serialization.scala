package com.github.jedesah.thrift

import org.apache.thrift.protocol.TProtocol

package object serialization {
  def read[T](protocol: TProtocol)(implicit codec: ThriftCodec[T]): T = codec.read(protocol)
  def write[T](obj: T, protocol: TProtocol)(implicit codec: ThriftCodec[T]) = codec.write(obj, protocol)

  trait ThriftCodec[T] {
    def read(protocol: TProtocol): T
    def write(obj: T, protocol: TProtocol)
  }

  object ThriftCodec {
    implicit val intCodec = new ThriftCodec[Int] {
      def read(protocol: TProtocol): Int = protocol.readI32()
      def write(obj: Int, protocol: TProtocol) { protocol.writeI32(obj) }
    }

    implicit val shortCodec = new ThriftCodec[Short] {
      def read(protocol: TProtocol): Short = protocol.readI16()
      def write(obj: Short, protocol: TProtocol) { protocol.writeI16(obj) }
    }
  }
}

