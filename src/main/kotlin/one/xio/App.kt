package one.xio

import kotlinx.coroutines.experimental.*
import java.nio.channels.*

class App(private var selector: Selector = Selector.open()) {
    fun listen(socket: SelectableChannel, att: Visitor? = null): SelectionKey {
        System.err.println("listener assigned")
        return socket.register(selector, SelectionKey.OP_ACCEPT, att)!!
    }

    suspend fun start(defaultVisitor: Visitor = Http1_1_Acceptor()) {
        while (selector.isOpen) {
            val select = selector.select()
            if (select > 0) {
                val selectedKeys = selector.selectedKeys()

                val map: List<Job> = selectedKeys.map {
                    launch {
                        val visitor: Visitor = (it.attachment() ?: defaultVisitor) as Visitor
                        visitor.resume(it)
                    }
                }
                map.forEach {
                    it.join()
                }
                selector.selectedKeys().clear()
            }
        }
    }
}