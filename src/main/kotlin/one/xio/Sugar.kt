/**
 * Syntactic Sugar for NIO objects
 */
package one.xio

import kotlinx.coroutines.experimental.channels.Channel
import java.lang.Character.*
import java.nio.*
import java.nio.ByteBuffer.*
import java.nio.channels.*


/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.debug get() = apply { debug("%%: " + dup.rew.str()) }

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.rew get() = apply { rewind() }


/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.dup get() = duplicate()!!


/** moves the cursor to get() the next non-ws byte */
tailrec fun ByteBuffer.forceSkipWs(): ByteBuffer =
        when {
            !hasRemaining() -> ZERO_BUFFER
            !isWhitespace(char) -> back1
            else -> forceSkipWs()
        };


/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
tailrec fun ByteBuffer.seekWs(): ByteBuffer =
        when {
            !hasRemaining() -> ZERO_BUFFER
            isWhitespace(get().toInt()) -> back1
            else -> seekWs()
        }


/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.skipWs
    get() = apply {
        var open = false;
        while (let { open = hasRemaining();open } && isWhitespace(char));
        if (open) back1
    }

/**
 * best attempt to find EOL
 */
tailrec fun ByteBuffer.toEol(): ByteBuffer =
        when {
            !hasRemaining() -> ZERO_BUFFER
            ch == '\n' -> this
            else -> toEol()
        }


/** moves the pointer back one  */
val ByteBuffer.back1
    get() = apply {
        when {
            0 < pos -> pos--
        }
    }

/**
 * moves thepointer back 2@see
 */
fun ByteBuffer.back2() = apply {
    if (1 < position()) position(position() - 2)
    else back1
}

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
fun ByteBuffer.noop() = apply { }

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
fun ByteBuffer.skipDigits() = apply {
    while (hasRemaining() && Character.isDigit(get().toInt()));
}

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.comp get() = compact()!!

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.cp get() = cat(ZERO_BUFFER, this.dup)

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.res get() = reset() as ByteBuffer

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.clr get() = clear() as ByteBuffer

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.ro get() = asReadOnlyBuffer()!!

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
tailrec fun ByteBuffer.pad0_(): ByteBuffer = when {
    !hasRemaining() -> this
    else -> this.put(0.toByte()).pad0_()
}

val ByteBuffer.pad0
    get() = pad0_()

/**
 * stuffs 0's into the buffer from 0->position
 */
val ByteBuffer.pad0UntilPosition
    get() = apply {
        val flip1 = dup.fl
        while (flip1.hasRemaining()) flip1.put(0.toByte());
    }

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.grow get() = ByteBuffer.allocateDirect(capacity() shl 1).put(this)!!

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.fl get() = flip()!! as ByteBuffer

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.mk get() = mark()!! as ByteBuffer

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
val ByteBuffer.sl get() = slice()!!

/**
 * does get 1 byte, returns 0xff & byte toChar
 */
val ByteBuffer.ch get() = int0xff.toChar()

/**
gets 1 byte as b32
 */
val ByteBuffer.b32 get() = get().toInt()

/**
 * gets 1 byte and returns b32&0xff
 */
val ByteBuffer.int0xff: Int
    get() = b32 and 0xff

/**  convenience method to reduce verbosity and extra cast operations for pre-jdk9 NIO  */
//fun ByteBuffer.lim():Int
//(a: Int) = limit(a) as ByteBuffer
var ByteBuffer.lim
    get() = limit()
    set(l) {
        limit(l)
    }
/**
 * goes from current position, creates a slice, moves the position. returns the slice with right-trim
 */
val ByteBuffer.nextLineBuf: ByteBuffer
    get() {
        val tmp = sl


        if (tmp.toEol() != ZERO_BUFFER) {
            pos += tmp.pos; tmp.rtrim.fl
        }
        return tmp
    }

/**
 * more convenience wrappers
 */
var ByteBuffer.pos
    get() = position()
    set(value) = let {
        position(value)
    }

/**
 * sets the position to before the current WS
 */
val ByteBuffer.rtrim
    get() = apply {
        val start = position()
        var i = start; while (0 <= --i && isWhitespace(get(i).toInt()));
        pos = (++i)
    }


/**
 * concatenates
 */
fun cat(vararg src: ByteBuffer) = allocateDirect(src.sumBy(ByteBuffer::remaining))!!.apply {
    assert(!src.isEmpty(), { "cannot cat with no args" });
    when {
        src.size == 1 -> return src[0]
        else -> src.forEach { put(it) }
    }
}

/**
 * for all intents and purposes NULL, NIL, EMPTY
 */
val ZERO_BUFFER: ByteBuffer = allocate(0).asReadOnlyBuffer()

/**
 *  if assertions are enabled, this prints the string to stderr
 */
fun debug(s: String) = try {
    assert(false)
} catch (ignored: AssertionError) {
    System.err.println(s)
}

/**
 * 12factor.net -- log to stderr, skip the rest.
 */
fun log(s: String) = System.err.println(s)

/**
 * macro for stringify to utf8
 *
 */
fun ByteBuffer.str() = Charsets.UTF_8.decode(this).toString()

fun java.nio.channels.SelectionKey.wakeup() {
    selector().wakeup()
}

infix operator fun SelectionKey.plusAssign(op: Int) {
    interestOps(interestOps() or op)
}

infix operator fun SelectionKey.minusAssign(op: Int) {
    interestOps(interestOps() and (0xff xor op))
}

fun SelectionKey.visit() {
    (visitor ?: SelectionKey::close)(this)
}

/** error response */
fun SelectionKey.simpleresponse(httpStatus: HttpStatus,
                                onSuccess: Visitor? = SelectionKey::close, vararg payload: ByteBuffer) {
    this += SelectionKey.OP_WRITE
    visitor = {
        val s = "HTTP/1.1 ${httpStatus.name} ${httpStatus.caption}\r\n"
        write(cat(s.buf, *payload).fl, onSuccess)
    }
    wakeup()
}

val String.buf: ByteBuffer
    get() = Charsets.UTF_8.encode(this)!!

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
    this += SelectionKey.OP_WRITE
    wakeup()
}

private fun SelectionKey.flush() = apply {
    this.socket.socket().getOutputStream().flush()
}

/** convenience method to write a buffer then stop the OP_WRITE interest then execute success Visitor */
fun SelectionKey.write(s: String, success: Visitor? = null) {
    val payload = Charsets.UTF_8.encode(s)
    write(payload, success)

}

var SelectionKey.visitor: Visitor?
    get() = attachment() as Visitor? ?: SelectionKey::close
    set(value) {
        attach(value)
    }

val SelectionKey.socket
    get() = channel() as SocketChannel
/**
 * sometimes you just can't keep it straight
 */
val ByteBuffer.guess: ByteBuffer
    get() = when {
        lim == 0 -> clr.also { assert(false, { "clear/rewind operation here" }) }
        pos == 0 -> this.also { assert(false, { "already flipped here" }) }
        else -> fl.also { assert(false, { "needs flip" }) }
    }

/** flush and close the underlying socket
 */
fun SelectionKey.close() {
    flush().socket.close()
}

suspend operator fun <E> Channel<E>.plus(read: E) = send(read)
/**
 * returns ZERO_BUFFER on overflow
 */
fun SelectionKey.read(payload: ByteBuffer = allocateDirect(512),
                      socketChannel: SocketChannel = channel() as SocketChannel): ByteBuffer {
    var read: Int = -1
    try {
        read = socketChannel.read(payload)
    } catch (e: Throwable) {
        this -= SelectionKey.OP_READ
    }
    return if (0 < read) payload
    else ZERO_BUFFER

}