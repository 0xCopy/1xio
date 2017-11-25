package one.xio

/** See  https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Request_methods
 */
enum class HttpMethod {
    /**The GET method requests a representation of the specified resource. Requests using GET should only retrieve data and should have no other effect. (This is also true of some other HTTP methods.)[1] The W3C has published guidance principles on this distinction, saying, "Web application design should be informed by the above principles, but also by the relevant limitations."[15] See safe methods below.*/
    GET,
    /**The HEAD method asks for a response identical to that of a GET request, but without the response body. This is useful for retrieving meta-information written in response headers, without having to transport the entire content.*/
    HEAD,
    /**The POST method requests that the server accept the entity enclosed in the request as a new subordinate of the web resource identified by the URI. The data POSTed might be, for example, an annotation for existing resources; a message for a bulletin board, newsgroup, mailing list, or comment thread; a block of data that is the result of submitting a web form to a data-handling process; or an item to add to a database.[16]*/
    POST,
    /**The PUT method requests that the enclosed entity be stored under the supplied URI. If the URI refers to an already existing resource, it is modified; if the URI does not point to an existing resource, then the server can create the resource with that URI.[17]*/
    PUT,
    /**The DELETE method deletes the specified resource.*/
    DELETE,
    /**The TRACE method echoes the received request so that a client can see what (if any) changes or additions have been made by intermediate servers.*/
    TRACE,
    /**The OPTIONS method returns the HTTP methods that the server supports for the specified URL. This can be used to check the functionality of a web server by requesting '*' instead of a specific resource.*/
    OPTIONS,
    /**[18] The CONNECT method converts the request connection to a transparent TCP/IP tunnel, usually to facilitate SSL-encrypted communication (HTTPS) through an unencrypted HTTP proxy.[19][20] See HTTP CONNECT tunneling.*/
    CONNECT,
    /**The PATCH method applies partial modifications to a resource.[21]*/
    PATCH,
}