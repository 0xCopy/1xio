package one.xio

import kotlinx.coroutines.experimental.*
import java.net.*
import java.nio.channels.*
import java.util.concurrent.*
import kotlin.coroutines.experimental.*

fun main(args: Array<String>) {
    val default = Visitor(EmptyCoroutineContext)
    val newCachedThreadPool = Executors.newCachedThreadPool()
    val app = App()

    launch(CommonPool) {
        app.dispatchSelector()
    }

    app.listen(ServerSocketChannel.open().bind(
            InetSocketAddress(8080))!!.configureBlocking(false)!!, default)

    val o = object : Object() {}
    synchronized(o) {
        try {
            o.wait()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }
}