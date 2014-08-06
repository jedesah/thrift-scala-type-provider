Scala Thrift Type Provider
============================

###Usage:

    @fromSchema("myThrift.thrift") object ChatIDL
   
    val msg = ChatIDL.Message("Hello")
    
    object MyImpl extends ChatIDL.Communication {
      def ping  = logPingReceived()
      def join(userId: ChatIDL.ID)
    }

###More elaborate example

Given this thrift file:

     struct Point {
        1: double x
     }
     
     struct Elem {
        1: i32 x
     }
     
     struct Missing {
        1: bool y
        2: list<string> aa
        3: Elem i
        4: set<bool> bb
        5: map<string, string> cc
     }
     
     union Thing {
        1: Point a
        2: Elem b
        3: Missing c
     }
     
     union OtherThig {
        1: Elem b
        2: string c
        3: Missing d
     }
     
     service Heartbeet {
        string ping(1: string greet, 2: Thing b)
     }
     
You could use the type provider like this:

     object MyApp extends App {
     
        import typeproviders.rdfs.public._
     
         @fromSchema("/dctype.rdf") object dct
         
         import dct._
         
         val a = Point(4)
         val b = Elem(3)
         val c = Missing(true,List("allo"), Elem(4), Set(true), Map("hey" -> "you"))
         println(a)
         println(b)
         println(a.x)
         
         object MyImpl extends Heartbeet {
            def ping(greet: String, c: Thing): String = "greetings!"
         }
         
         val h: Heartbeet = MyImpl
         h.ping("hello", Thing.a(Point(4)))
     }
