package one.xio

import kotlinx.coroutines.experimental.*
import java.nio.channels.*
import java.nio.channels.SelectionKey.*
import kotlin.coroutines.experimental.*


@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
open class Visitor(override val context: CoroutineContext = CommonPool) : Continuation<SelectionKey> {
    override fun resumeWithException(exception: Throwable) {
        exception.printStackTrace()
    }

    override fun resume(key: SelectionKey) = operationOrder.forEach {
        if (it.first.and(other = key.interestOps()) != 0) {
            val async = async { it.second(key) }
        }
    }

    open fun onAccept(key: SelectionKey) = Companion.reserved(key)
    open fun onConnect(key: SelectionKey) = Companion.reserved(key)
    open fun onRead(key: SelectionKey) = Companion.reserved(key)
    open fun onWrite(key: SelectionKey) = Companion.reserved(key)

    /**
     * optimized for serving http GET by accepting->read->write,
     */
    open var operationOrder = arrayOf(
            Pair(OP_ACCEPT, this::onAccept),
            Pair(OP_READ, this::onRead),
            Pair(OP_WRITE, this::onWrite),
            Pair(OP_CONNECT, this::onConnect)//unneccesary at the same time as Listen
    )

    companion object {
        val reserved: (SelectionKey) -> Unit = { TODO("not implemented") }
    }
}

