package one.xio

import java.io.*
import java.lang.Math.*
import java.net.*
import java.nio.*
import java.nio.channels.*
import java.nio.charset.StandardCharsets.*
import java.util.*
import java.util.Arrays.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * this is a utility class to parse a HttpRequest header or $res header according to declared need of header/cookies
 * downstream.
 *
 *
 * since java string parsing can be expensive and addHeaderInterest can be numerous this class is designed to parse only
 * what is necessary or typical and enable slower dynamic grep operations to suit against a captured [ByteBuffer]
 * as needed (still cheap)
 *
 *
 * preload addHeaderInterest and cookies, send $res and HttpRequest initial onRead for .read()
 *
 *
 *
 *
 *
 *
 * User: jim Date: 5/19/12 Time: 10:00 PM
 */
open class Rfc822HeaderState  {


}