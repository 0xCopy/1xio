package one.xio

import kotlinx.coroutines.experimental.*
import java.nio.channels.*

class App {
    fun listen(socket: SelectableChannel, att: Visitor? = null) = socket.register(selector,
                                                                                  SelectionKey.OP_ACCEPT, att)!!
    private var selector: Selector = Selector.open()


    fun dispatchSelector(defaultVisitor: Visitor = Visitor()) {
        val s: Selector = selector
        while (s.isOpen) {
            val select = s.select()
            if (select > 0) {
                val selectedKeys = s.selectedKeys()
                selectedKeys.forEach {
                    launch {
                        val visitor: Visitor = (it.attachment() ?: defaultVisitor) as Visitor
                        visitor.resume(it)
                    }
                }
                s.selectedKeys().clear()
            }
            continue
        }
    }
}