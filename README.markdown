Scala Thrift Type Provider
============================

Usage:

    @fromSchema("myThrift.thrift") object ChatIDL
   
    val msg = ChatIDL.Message("Hello")
    
    object MyImpl extends ChatIDL.Communication {
      def ping  = logPingReceived()
      def join(userId: ChatIDL.ID)
    }
