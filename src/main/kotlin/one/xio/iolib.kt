package one.xio

import java.nio.*

val ZERO_BUFFER: ByteBuffer = ByteBuffer.allocate(0).asReadOnlyBuffer()

private fun debug(s: String) = try {
    assert(false)
} catch (ignored: AssertionError) {
    System.err.println(s)
}

fun log(s: String) = System.err.println(s)

fun str( byteBuffer: ByteBuffer) = Charsets.UTF_8.decode(byteBuffer).toString()