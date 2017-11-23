package one.xio

import kotlinx.coroutines.experimental.*
import java.net.*
import java.nio.channels.*
import kotlin.coroutines.experimental.*

public fun main(args: Array<String>) { 
    val app = App() 
    launch { app.start() }
    app.listen(ServerSocketChannel.open().bind(InetSocketAddress(8080))!!.configureBlocking(false)!!, Http1_1_Acceptor()) 
    val lock = Object()
    synchronized(lock) {
        lock.wait()
    }

}

