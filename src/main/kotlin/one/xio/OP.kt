//package one.xio
//
//enum class OP(val bit:Int)
//{
//   /**
//    * Operation-set bit for read operations.
//    *
//    * <p> Suppose that a selection key's interest set contains
//    * <tt>OP_READ</tt> at the start of a <a
//    * href="Selector.html#selop">selection operation</a>.  If the selector
//    * detects that the corresponding channel is ready for reading, has reached
//    * end-of-stream, has been remotely shut down for further reading, or has
//    * an error pending, then it will add <tt>OP_READ</tt> to the key's
//    * ready-operation set and add the key to its selected-key&nbsp;set.  </p>
//    */
//   OP_READ(1 shl 0),
//
//   /**
//    * Operation-set bit for write operations.
//    *
//    *
//    *  Suppose that a selection key's interest set contains
//    * <tt>OP_WRITE</tt> at the start of a [selection operation](Selector.html#selop).  If the selector
//    * detects that the corresponding channel is ready for writing, has been
//    * remotely shut down for further writing, or has an error pending, then it
//    * will add <tt>OP_WRITE</tt> to the key's ready set and add the key to its
//    * selected-key&nbsp;set.
//    */
//   OP_WRITE(1 shl 2),
//
//   /**
//    * Operation-set bit for socket-connect operations.
//    *
//    *
//    *  Suppose that a selection key's interest set contains
//    * <tt>OP_CONNECT</tt> at the start of a [selection operation](Selector.html#selop).  If the selector
//    * detects that the corresponding socket channel is ready to complete its
//    * connection sequence, or has an error pending, then it will add
//    * <tt>OP_CONNECT</tt> to the key's ready set and add the key to its
//    * selected-key&nbsp;set.
//    */
//   OP_CONNECT(1 shl 3),
//
//   /**
//    * Operation-set bit for socket-accept operations.
//    *
//    *
//    *  Suppose that a selection key's interest set contains
//    * <tt>OP_ACCEPT</tt> at the start of a [selection operation](Selector.html#selop).  If the selector
//    * detects that the corresponding server-socket channel is ready to accept
//    * another connection, or has an error pending, then it will add
//    * <tt>OP_ACCEPT</tt> to the key's ready set and add the key to its
//    * selected-key&nbsp;set.
//    */
//   OP_ACCEPT(1 shl 4);
//    fun   toInt(): Int {return bit;}
//   companion object {
//      private val map =  values().associateBy( OP::bit);
//      fun fromInt(type: Int) = map[type]
//   }
//}