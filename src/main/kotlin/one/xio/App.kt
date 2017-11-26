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

    fun start(defaultVisitor: (SelectionKey) -> Unit?) {
        while (selector.isOpen) {
            val select = selector.select(5000)
            if (select > 0) {
                val selectedKeys = selector.selectedKeys()

                runBlocking {
                    val map: List<Job> = selectedKeys.map { key2 ->
                        val visitor: Visitor = (key2.attachment() ?: defaultVisitor) as Visitor
                        val job = launch {
                            log("launched visitor " + visitor)
                            visitor.invoke(key2)
                        }
                        job
                    }
                    map.forEach {
                        it.join()
                    }
                }
                selector.selectedKeys().clear()
            }
        }
    }
}

/** serving http 1.1 starts with Accept, then READ, then ussually WRITE, and sometimes with Responses 101 there's back forth.
 */
fun http1_1_Acceptor(it: SelectionKey) {
    /** this is the ACCEPTOR
     *
     */


    val serverSocketChannel = it.channel() as ServerSocketChannel
    val newChannel = serverSocketChannel.accept()

    newChannel?.run {
        configureBlocking(false)
        System.err.println("connection from " + remoteAddress)

        val allReads = Channel<ByteBuffer>(Channel.UNLIMITED)

        launch {
            val headerLines = Channel<ByteBuffer>()

            var input: ByteBuffer? = null

            while (true) {
                val receive = allReads.receive()

                val cursor = when {
                    input != null -> cat(input, receive)
                    else -> receive.apply { input = receive.cp() }
                }
                do {
                    var stillSpace = true
                    var foundLF = false
                    while (let { stillSpace = cursor.hasRemaining();stillSpace } && !let { foundLF = cursor.mk().get().toInt() and 0xff != '\n'.toInt();foundLF }) {
                    };
                    when {
                        !stillSpace -> cursor.flip()
                        else -> if (foundLF) {
                            val position1 = cursor.position()
                            cursor.rtrim()
                            val amtOfWhiteSpace = position1 - cursor.position()
                            when {
                                3 > amtOfWhiteSpace/*we found the header end.*/ -> return@launch
                                else -> headerLines.send(/*str*/(/*cat*/(/*EMPTY_SET, */cursor.fl().slice())))
                            }
                            cursor.skipWs();
                        }
                    }
                } while (cursor.hasRemaining())
                input = cursor;
            }
        }

        /**
         * this is our READ
         */
        it.spawn(newChannel, SelectionKey.OP_READ, {
            val read = it.read()
            read?.apply {
                runBlocking {
                    allReads + read.dup().fl() // flip ops here in blocking context.

                }

            }
        })

    }
}


operator fun ByteBuffer.plusAssign(receive: ByteBuffer) {
    put(receive)
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
    launch { app.start(::http1_1_Acceptor) }
    launch {
        delay(500)
        app.listen(ServerSocketChannel.open().bind(
                InetSocketAddress(8080))!!.configureBlocking(false)!!, ::http1_1_Acceptor)
    }
    val lock = Object()
    synchronized(lock) {
        lock.wait()
    }

}


class SkipException : Throwable()

