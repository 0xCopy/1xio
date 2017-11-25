package one.xio

import java.nio.*

/**
 * some kind of less painful way to do byteBuffer operations and a few new ones thrown in.
 *
 *
 * evidence that this can be more terse than what jdk pre-8 allows:
 * <pre>
 *
 * res.add(bb(nextChunk, rewind));
 * res.add((ByteBuffer) nextChunk.rewind());
 *
 *
</pre> *
 */
//enum class Cursive : bub {
typealias bub = (ByteBuffer) -> ByteBuffer/**
 * convenience method
 *
 * @param bytes
 * @param operations
 * @return
 */
fun str(bytes: ByteBuffer, vararg operations: bub): String {
    val bb = bb(bytes, *operations)
    return Charsets.UTF_8.decode(bb).toString()
}

tailrec fun bb(target: ByteBuffer, vararg op: bub): ByteBuffer {

    return when {
        op.size == 0 -> target
        op.size == 1 -> op.first()(target)
        else -> bb(
                (op.first())(target),
                *op.sliceArray(IntRange(1, op.size)))
    }


}

/**
 * User: jim
 * Date: Oct 6, 2007
 * Time: 3:10:32 AM
 */
class Allocator(vararg bytes: Int) {


    private var initialCapacity = Runtime.getRuntime().availableProcessors() * 20 * 2


    val EMPTY_SET = ByteBuffer.allocate(0).asReadOnlyBuffer()

    internal var size = initialCapacity

    val isDirect: Boolean
        get() = false
    private var _DIRECT_HEAP: ByteBuffer? = null
    internal var DIRECT_HEAP: ByteBuffer?
        get() = _DIRECT_HEAP
        set(value) {
            _DIRECT_HEAP = value
        }

    init {
        if (bytes.size > 0)
            initialCapacity = bytes[0]

        var buffer: ByteBuffer? = null
        while (buffer == null)
            try {

                if (isDirect)
                    buffer = ByteBuffer.allocateDirect(size).limit(0) as ByteBuffer
                else
                    buffer = ByteBuffer.allocate(size).limit(0) as ByteBuffer

                DIRECT_HEAP = buffer
                log("Heap allocated at " + size / MEG + " megs")
                size *= 2

            } catch (e: IllegalArgumentException) {
                size = Math.max(16 * MEG, size / 2)
                System.gc()
            } catch (e: OutOfMemoryError) {
                size = Math.max(16 * MEG, size / 2)
                System.gc()
            }

    }

    private fun init() {

        var buffer: ByteBuffer? = null
        while (buffer == null)
            try {

                if (isDirect)
                    buffer = ByteBuffer.allocateDirect(size).limit(0) as ByteBuffer
                else
                    buffer = ByteBuffer.allocate(size).limit(0) as ByteBuffer

                DIRECT_HEAP = buffer
                log("Heap allocated at " + size / MEG + " megs")
                size *= 2

            } catch (e: IllegalArgumentException) {
                size = Math.max(16 * MEG, size / 2)
                System.gc()
            } catch (e: OutOfMemoryError) {
                size = Math.max(16 * MEG, size / 2)
                System.gc()
            }

    }

    private fun debug(s: String) = try {
        assert(false)
    } catch (ignored: AssertionError) {
        System.err.println(s)
    }
    private fun log(s: String) = System.err.println(s)

    internal fun allocate(size: Int): ByteBuffer {
        if (size == 0) return EMPTY_SET
        try {
            DIRECT_HEAP?.limit(DIRECT_HEAP!!.limit() + size)
        } catch (e: IllegalArgumentException) {
            init()
            return allocate(size)
        }

        val ret = DIRECT_HEAP?.slice()?.limit(size)?.mark() as ByteBuffer
        DIRECT_HEAP?.position(DIRECT_HEAP!!.limit())
        return ret
    }

    companion object {
        var MEG = 1 shl 10 shl 10
        var BLOCKSIZE = MEG * 2
    }

}
