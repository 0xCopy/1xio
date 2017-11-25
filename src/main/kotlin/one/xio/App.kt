package one.xio

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import java.net.*
import java.nio.*
import java.nio.channels.*


typealias Visitor = (SelectionKey) -> Unit

class App(private var selector: Selector = Selector.open()) {
    fun listen(socket: SelectableChannel, att: (SelectionKey) -> Unit): SelectionKey {
        System.err.println("listener assigned")
        return socket.register(selector, SelectionKey.OP_ACCEPT, att)!!
    }

    suspend fun start(defaultVisitor: (SelectionKey) -> Unit?) {
        while (selector.isOpen) {
            val select = selector.select()
            if (select > 0) {
                val selectedKeys = selector.selectedKeys()

                val map: List<Job> = selectedKeys.map {
                    launch {
                        val visitor: Visitor = (it.attachment() ?: defaultVisitor) as Visitor
                        visitor.invoke(it)
                    }
                }
                map.forEach {
                    it.join()
                }
                selector.selectedKeys().clear()
            }
        }
    }
}

/** serving http 1.1 starts with Accept, then READ, then ussually WRITE, and sometimes with Responses 101 there's back forth.
 */
fun getHttp1_1_Acceptor(): Visitor = { key: SelectionKey ->
    /** this is the ACCEPTOR
     *
     */


    val serverSocketChannel = key.channel() as ServerSocketChannel
    val newChannel = serverSocketChannel.accept()

    newChannel?.run {
        configureBlocking(false)
        System.err.println("connection from " + remoteAddress)

        val allReads = Channel<ByteBuffer>()

        var cursor: ByteBuffer? = null


        launch {
            val lines = Channel<ByteBuffer>()

            while (true) {
                val receive = allReads.receive()
                cursor =  cursor ?: receive
                try {
                    bb(cursor!!, mark, toEol)
                }catch (e:Throwable){
                    bb(cursor!!,reset)
                }
            }
        }

        /**
         * this is our READ
         */
        key.spawn(newChannel, SelectionKey.OP_READ, {
            val read = key.read()
            read?.apply {
                runBlocking {
                    allReads + read
                    bb(this@apply, skipWs)
                }

            }
        })

    }
}

 suspend operator fun <E> Channel<E>.plus(read: E) = send(read)


 fun SelectionKey.read(payload: ByteBuffer = ByteBuffer.allocateDirect(512)): ByteBuffer? {
    val socketChannel = this.channel() as SocketChannel
    if (0 != socketChannel.read(payload))
        return payload
    return null

}

fun SelectionKey.spawn(newChannel: SocketChannel, interestOps: Int, visitor: (SelectionKey) -> Unit): Unit {
    newChannel.register(this.selector(), interestOps, visitor)
}

fun main(args: Array<String>) {
    val app = App()
    launch { app.start(getHttp1_1_Acceptor()) }
    launch {

        delay(500)
        app.listen(ServerSocketChannel.open().bind(
                InetSocketAddress(8080))!!.configureBlocking(false)!!, getHttp1_1_Acceptor())

    }
    val lock = Object()
    synchronized(lock) {
        lock.wait()
    }

}


class SkipException : Throwable()

