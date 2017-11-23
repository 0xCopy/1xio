package one.xio

import java.nio.*
import java.nio.channels.*
import java.nio.channels.SelectionKey.*

class Http1_1_Acceptor : Visitor {

    override fun resume(key: SelectionKey) {

        try {
            val serverSocketChannel = key.channel() as ServerSocketChannel

            val newChannel = serverSocketChannel.accept()

            newChannel?.run {
                configureBlocking(false)
                System.err.println("connection from " + remoteAddress)
                register(key.selector(), OP_READ, object : Visitor  {
                    override fun resume(key: SelectionKey) {
                        val buffer = ByteBuffer.allocateDirect(4096)
                        val read = newChannel.read(buffer)
                        if (read > 0) {
                            println(Charsets.UTF_8.decode(buffer.flip() as ByteBuffer?))
                            key.cancel()
                        }
                    }
                })
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

