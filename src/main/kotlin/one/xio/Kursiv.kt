package one.xio

import java.nio.*

val duplicate: bub = ByteBuffer::duplicate
val flip: bub = { it.flip() as ByteBuffer }
val slice: bub = ByteBuffer::slice
val mark: bub = { it.mark() as ByteBuffer }
val debug : bub= { it: ByteBuffer ->
        System.err.println("%%: " + str(it, duplicate, rewind))
         it
    }

val forceSkipWs: bub = {
    val position = it.position()
    while (it.hasRemaining() && Character.isWhitespace(
            it.get().toInt())); if (!it.hasRemaining()) {
    it.position(position); throw BufferUnderflowException()
}
    bb(it, back1)
}
val toWs: bub = { target ->
    while (target.hasRemaining() && !Character.isWhitespace(target.get().toInt())) {
    }
    target
}

object skipWs : bub {
    override fun invoke(p1: ByteBuffer): ByteBuffer {
        var rem = false
        var captured = false
        var r = false

        while (
        let { rem = p1.hasRemaining();rem } &&
                let {
                    val other = let {
                        r = Character.isWhitespace(0xff.and((p1.mark() as ByteBuffer).get().toInt()))
                        r
                    }
                    captured = captured||(other)
                    captured
                } && r);
        when {
            captured && rem -> return p1.reset() as ByteBuffer
            captured -> p1
            else -> throw SkipException()
        }

        return p1
    }

}

/**
 * best attempt to find EOL
 */
val toEol: bub = {
    while (it.hasRemaining() && '\n'.toByte() != it.get());
    it
}
val back1: bub = {
    val position = it.position()
    (if (0 < position) it.position(position - 1) else it) as ByteBuffer
}
val back2: bub = {
    val position = it.position()
    (if (1 < position) it.position(position - 2) else bb(it,
                                                                 back1)) as ByteBuffer
}
val rtrim: bub = {
    val start = it.position()
    var i = start; while (0 <= --i && Character.isWhitespace(it.get(i).toInt()));
    it.position(++i) as ByteBuffer
}

val noop : bub ={  it }

val skipDigits: bub = {
    while (it.hasRemaining() && Character.isDigit(it.get().toInt()));
    it
}
val compact: bub = ByteBuffer::compact
val reset: bub = { it.reset() as ByteBuffer }
val rewind: bub = { it.rewind() as ByteBuffer }
val clear: bub = { it.clear() as ByteBuffer }
val ro: bub = ByteBuffer::asReadOnlyBuffer
val pad0: bub = {
    while (it.hasRemaining()) it.put(0.toByte())
    it
}
val pad0Until: bub = {
    val limit = it.limit(); it.flip(); while (it.hasRemaining()) it.put(0.toByte()); it.limit(
        limit) as ByteBuffer
}
val grow: bub = { ByteBuffer.allocateDirect(it.capacity() shl 1).put(it) }
