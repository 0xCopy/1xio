//@file:Suppress("UNUSED_EXPRESSION")
package one.xio

import java.lang.Character.*
import java.nio.*
import java.nio.ByteBuffer.*

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
    set(l){
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