@file:Suppress("UNUSED_EXPRESSION")

package one.xio

import java.nio.*
import java.nio.ByteBuffer.*


fun ByteBuffer.debug() = apply { System.err.println("%%: " + str(dup().rew())) }

private fun ByteBuffer.rew() = apply { rewind() }


 fun ByteBuffer.dup() = duplicate() as ByteBuffer


/** moves the cursor to get() the next non-ws byte */
fun ByteBuffer.forceSkipWs() = apply {
    val position = position()
    while (hasRemaining() && Character.isWhitespace(get().toInt())) {
    };
    when {
        !hasRemaining() -> {
            position(position);
            throw BufferUnderflowException()
        }
        else -> back1()
    }
}


fun ByteBuffer.seekWs() = apply {
    while (hasRemaining() && !Character.isWhitespace(get().toInt()));
}

fun ByteBuffer.skipWs() = apply {

    var rem = false
    var captured = false
    var r = false
    while (run { rem = hasRemaining(); rem } && run {
        run {
            r = Character.isWhitespace(0xff.and((mk()).get().toInt())); r
        }; captured = captured || r; captured
    } && r);

    when {
        captured && rem -> reset()
        captured -> this
        else -> throw SkipException()
    }

}


/**
 * best attempt to find EOL
 */
fun ByteBuffer.toEol() = apply {
    while (hasRemaining() && '\n'.toByte() != get());
}

/** moves the pointer back one  */
fun ByteBuffer.back1(): ByteBuffer = apply {
    val position = position()
    when {
        0 < position -> position(position - 1)
    }
}

/**
 * moves thepointer back 2@see
 */
fun ByteBuffer.back2() = apply {
    val position = position()
    when {
        1 < position -> position(position - 2)
        else -> back1()
    }
}

fun ByteBuffer.noop() = this

fun ByteBuffer.skipDigits() = apply { while (hasRemaining() && Character.isDigit(get().toInt())); }

fun ByteBuffer.comp() = compact()!!
fun ByteBuffer.cp(): ByteBuffer  = cat(ZERO_BUFFER, this.dup())


fun ByteBuffer.res() = reset() as ByteBuffer
fun ByteBuffer.clr() = clear() as ByteBuffer
fun ByteBuffer.ro() = asReadOnlyBuffer()!!

fun ByteBuffer.pad0() = apply {
    while (hasRemaining()) put(0.toByte())
}

/**
 * stuffs 0's into the buffer from 0->position
 */
fun ByteBuffer.pad0UntilPosition() = apply {
    val flip1 = dup().fl()
    while (flip1.hasRemaining()) flip1.put(0.toByte());
}

fun ByteBuffer.grow() = ByteBuffer.allocateDirect(capacity() shl 1).put(this)!!

fun ByteBuffer.fl() = flip() as ByteBuffer
fun ByteBuffer.mk() = mark() as ByteBuffer
fun ByteBuffer.lim(a: Int) = apply{limit(a)}
/**
 * sets the position to before the current WS
 */
fun ByteBuffer.rtrim() = apply {
    val start = position()
    var i = start; while (0 <= --i && Character.isWhitespace(get(i).toInt()));
    position(++i) as ByteBuffer
}

fun cat(vararg src: ByteBuffer) = allocateDirect(src.sumBy(ByteBuffer::remaining))!!.apply {
    assert (!src.isEmpty(),{"cannot cat with no args"});
    when {
        src.size==1 -> return src[0]
        else -> src.forEach { put(it) }
    }
}
