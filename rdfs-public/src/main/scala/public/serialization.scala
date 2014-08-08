package com.github.jedesah.thrift

import org.apache.thrift.protocol.TProtocol

package object serialization {
  def read[T](protocol: TProtocol)(implicit codec: ThriftCodec[T]): T = codec.read(protocol)
  def write[T](obj: T, protocol: TProtocol)(implicit codec: ThriftCodec[T]) = codec.write(obj, protocol)

  trait ThriftCodec[T] {
    def read(protocol: TProtocol): T
    def write(obj: T, protocol: TProtocol)
  }
}

