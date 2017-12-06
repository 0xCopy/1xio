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
import kotlin.text.Charsets.UTF_8


typealias Visitor = (SelectionKey) -> Unit

fun main(args: Array<String>) {
    val app = App()
    launch { app.start(::http1_1_Acceptor) }
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

class App(private var selector: Selector = Selector.open()) {
    fun listen(socket: SelectableChannel, att: Visitor): SelectionKey
            = socket.register(selector,
                              SelectionKey.OP_ACCEPT,
                              att).apply {
        System.err.println("listener assigned")
    }

    fun start(defaultVisitor: Visitor) {
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

fun SelectionKey.visit() {
    visitor(this)
}

/** serving http 1.1 starts with Accept, then READ, then ussually WRITE, and sometimes with Responses 101 there's back forth.
 */
fun http1_1_Acceptor(it: SelectionKey) {
    /** this is the ACCEPTOR
     *
     */
    val serverSocketChannel = it.channel() as ServerSocketChannel
    accept(serverSocketChannel.accept(), it)
}

/**  */
fun accept(
        newChannel: SocketChannel,
        serverSelectorKey: SelectionKey
) {
    newChannel.configureBlocking(false)
    val allReads = Channel<ByteBuffer>(Channel.UNLIMITED)
    val newKey = newChannel.register(
            serverSelectorKey.selector(),
            OP_READ,
            { it: SelectionKey ->
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
    /**
     * controller channel
     */
    launch {
        while (!controller.isClosedForReceive) {
            val methodLine = controller.receive().debug
            val list = methodLine.str().split(Regex("\\s+"))
            methodLine.noop()


            val method: HttpMethod? = HttpMethod.valueOf(list[0])
            when (method) {
                null -> newKey.simpleresponse(`401`)
                HttpMethod.GET -> {
                    val path = list[1]
                    debug("get: path is " + path)
                    val path1 = Paths.get(".", path)
                    if (Files.exists(path1)) {

                        val toFile = path1.toFile()
                        var f = RandomAccessFile(toFile, "r").channel.map(FileChannel.MapMode.READ_ONLY, 0,
                                                                          toFile.length())!!

                        val clen = f.remaining().toString()
                        val contentType = (MimeType.valueOf(
                                path1.toString().split('.').last()) ?: MimeType.bin).contentType
                        newKey.simpleresponse(
                                `200`,
                                SelectionKey::close,
                                `Content-Length` + clen,
                                `Content-Type` + contentType,
                                "\r\n".buf,
                                f
                        )


                    }
                }

                else -> debug("method is $method")
            }
        }
    }
}


/** error response */
fun SelectionKey.simpleresponse(httpStatus: HttpStatus,
                                onSuccess: Visitor? = SelectionKey::close, vararg payload: ByteBuffer) {
    this += OP_WRITE
    visitor = {
        val s = "HTTP/1.1 ${httpStatus.name} ${httpStatus.caption}\r\n"
        write(cat(s.buf, *payload).fl, onSuccess)
    }
    wakeup()
}

val String.buf: ByteBuffer
    get() = UTF_8.encode(this)!!


fun SelectionKey.wakeup() {
    selector().wakeup()
}


infix operator fun SelectionKey.plusAssign(op: Int) {
    interestOps(interestOps() or op)
}


fun SelectionKey.write(payload: ByteBuffer, success: Visitor? = null) {
//    payload.guess
    debug(payload.toString())
    visitor = {
        if (payload.hasRemaining())
            socket.write(payload)
        if (payload.hasRemaining().not()) {
            visitor = success ?: { it.flush().close() }
        }
    }
    this += OP_WRITE
    wakeup()
}

private fun SelectionKey.flush() = apply {
    this.socket.socket().getOutputStream().flush()
}

/** convenience method to write a buffer then stop the OP_WRITE interest then execute success Visitor */
fun SelectionKey.write(s: String, success: Visitor? = null) {
    val payload = UTF_8.encode(s)
    write(payload, success)

}


infix operator fun SelectionKey.minusAssign(op: Int) {
    interestOps(interestOps() and (0xff xor op))
}

var SelectionKey.visitor: Visitor
    get() = attachment() as Visitor? ?: SelectionKey::close
    set(value: Visitor) {
        attach(value)
    }


val SelectionKey.socket
    get() = channel() as SocketChannel


fun response(function: HttpStatus) {
    /*
HTTP/1.1 200 OK
Accept-Ranges: bytes
Content-Length: 43
Content-Type: image/gif
ETag: "YM:1:5da9dff4-a9ff-454b-b30d-ec6b3ef64a220004ce767373b56b"
Last-Modified: Wed, 14 Nov 2012 15:47:25 GMT
Server: ATS
x-ysws-request-id: 938f60e4-e053-4557-a76f-8335c9cf7d99
x-ysws-visited-replicas: gops.usw44.mobstor.vip.gq1.yahoo.com
Y-Trace: BAEAQAAAAACGWWyjBeW2XAAAAAAAAAAA6ucqw3Evz2gAAAAAAAAAAAAFL_ST2myKAAUv9JPabP2pAQc.AAAAAA--
Cache-Control: public, max-age=273889468
Expires: Mon, 10 Aug 2026 03:57:10 GMT
Date: Tue, 05 Dec 2017 03:32:42 GMT
Connection: keep-alive
 */

}

/**
 * sometimes you just can't keep it straight if
 */

val ByteBuffer.guess: ByteBuffer
    get() = when {
        lim == 0 -> clr.also { assert(false, { "clear/rewind operation here" }) }
        pos == 0 -> this.also { assert(false, { "already flipped here" }) }
        else -> fl.also { assert(false, { "needs flip" }) }
    }

fun SelectionKey.close() {
    channel()?.close()
}

suspend operator fun <E> Channel<E>.plus(read: E) = send(read)

/**
 * returns ZERO_BUFFER on overflow
 */
fun SelectionKey.read(payload: ByteBuffer = ByteBuffer.allocateDirect(512),
                      socketChannel: SocketChannel = channel() as SocketChannel): ByteBuffer {
    var read: Int = -1
    try {
        read = socketChannel.read(payload)
    } catch (e: Throwable) {
        this -= OP_READ
    }
    return if (0 < read) payload
    else ZERO_BUFFER

}