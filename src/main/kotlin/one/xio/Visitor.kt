package one.xio

import java.nio.channels.*


 
interface Visitor {
    fun resume(key: SelectionKey)
}
 