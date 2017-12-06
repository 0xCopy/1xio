package one.xio

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import one.xio.HttpHeaders.*
import one.xio.HttpStatus.*
import java.io.*
import java.net.*
import java.nio.*
import java.nio.channels.*
import java.nio.channels.SelectionKey.*
import java.nio.file.*


fun main(args: Array<String>) {
    val app = App()
    launch { app.start() }
    launch {
        //        delay(500)
        app.listen(ServerSocketChannel.open().bind(
                InetSocketAddress(8080))!!.configureBlocking(false)!!, ::http1_1_Acceptor)
    }
    /* launch { delay(500)
     URL("http://localhost:8080").content

     }*/

    val lock = Object()
    synchronized(lock) {
        lock.wait()
    }
}

typealias Visitor = (SelectionKey) -> Unit

class App(private var selector: Selector = Selector.open()) {
    fun listen(socket: SelectableChannel, acceptor: Visitor): SelectionKey
            = socket.register(selector,
                              SelectionKey.OP_ACCEPT,
                              acceptor).apply {
        debug("listener assigned")
    }

    fun start() {
        while (selector.isOpen) {
            val select = selector.select(250)
            if (select > 0) {
                val selectedKeys: MutableSet<SelectionKey> = selector.selectedKeys()

                selectedKeys.forEach { it: SelectionKey ->
                    val visitor = it.visitor
                    it.visit()
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
    newChannel.configureBlocking(false)
    val allReads = Channel<ByteBuffer>(Channel.UNLIMITED)
    val newKey = newChannel.register(
            it.selector(),
            OP_READ,
            {
                runBlocking {
                    val read: ByteBuffer = it.read()
                    if (read != ZERO_BUFFER)
                        allReads.send(read)
                }

            }
    )!!
    val controller = Channel<ByteBuffer>(Channel.UNLIMITED)
    launch {
        var last: ByteBuffer? = null
        var inbound: ByteBuffer? = null
        while (!allReads.isClosedForReceive)
            loop@ while (true) {
                val src = allReads.receive()//recv allReads, not yet flipped
                val src1 = src.fl
                val inb = cat(
                        last ?: ZERO_BUFFER,
                        src1
                ).fl // create the fresh copy with trailing bytes cat'd
                val tmp = inb.nextLineBuf

                if (ZERO_BUFFER == tmp || !tmp.hasRemaining()) {
                    last = cat(inb.res)
                    break@loop
                }
                controller.send(tmp)
            }
    }
    launch {
        with(newKey) {
            while (!controller.isClosedForReceive) {
                val methodLine = controller.receive().debug
                val list = methodLine.str().split(Regex("\\s+"))
                methodLine.noop()


                val method: HttpMethod? = HttpMethod.valueOf(list[0])
                when (method) {
                    null -> simpleresponse(`401`)
                    HttpMethod.GET -> {
                        val path = list[1]
                        debug("GET: path is " + path)
                        val path1 = Paths.get(".", path)
                        if (Files.exists(path1)) {
                            val toFile = path1.toFile()
                            val channel = RandomAccessFile(toFile, "r").channel
                            val map = channel.map(FileChannel.MapMode.READ_ONLY, 0,
                                                  toFile.length())!!

                            val clen = map.remaining().toString()
                            val contentType = (
                                    MimeType.valueOf(path1.toString().split('.').last()) ?: MimeType.bin).contentType
                            simpleresponse(
                                    `200`,
                                    SelectionKey::close,
                                    `Content-Length` + clen,
                                    `Content-Type` + contentType,
                                    "\r\n".buf,
                                    map
                            )
                        } else
                            simpleresponse(`404`)
                    }

                    else -> {
                        debug("method is $method")
                        newKey.simpleresponse(`400`)
                    }
                }
            }
        }
    }
}

