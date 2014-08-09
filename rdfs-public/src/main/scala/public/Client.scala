package com.github.jedesah.thrift

import org.apache.thrift.TApplicationException
import org.apache.thrift.protocol.{TMessageType, TMessage, TProtocol}
import serialization._

class Client(protocol: TProtocol) {

  var seqId = 0

  protected def send[T: ThriftCodec](methodName: String, args: T) {
    seqId += 1
    protocol.writeMessageBegin(new TMessage(methodName, TMessageType.CALL, seqId))
    write(args, protocol)
    protocol.writeMessageEnd()
    protocol.getTransport.flush()
  }

  protected def receive[T: ThriftCodec](methodName: String): T = {
    val msg = protocol.readMessageBegin()
    if (msg.`type` == TMessageType.EXCEPTION) {
      val exception = TApplicationException.read(protocol)
      protocol.readMessageEnd()
      throw exception
    }
    if (msg.seqid != seqId) {
      val explanation = s"$methodName failed: out of sequence response"
      throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, explanation)
    }
    val result: T = read(protocol)
    protocol.readMessageEnd()
    result
  }
}
