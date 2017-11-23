package  one.xio

import java.lang.Character.*
import java.net.*
import java.nio.*
import java.nio.charset.*

/**
 * iana http headers produced from shell command below.
 */
// curl http://www.iana.org/assignments/message-headers/perm-headers.csv http://www.iana.org/assignments/message-headers/prov-headers.csv | tr -d '\r'| grep ,http,|while read; do echo '/**' $(echo $REPLY|cut -f5- -d, ) '*/' $(echo $REPLY|cut -f1 -d,|sed 's,-,-,g') ,;echo $a;done

enum class HttpHeaders {
    /** [RFC4229]  */
    `A-IM`,

    /** "[RFC7231, Section 5.3.2]"  */
    Accept,

    /** [RFC4229]  */
    `Accept-Additions`,

    /** "[RFC7231, Section 5.3.3]"  */
    `Accept-Charset`,

    /** [RFC7089]  */
    `Accept-Datetime`,

    /** "[RFC7231, Section 5.3.4][RFC7694, Section 3]"  */
    `Accept-Encoding`,

    /** [RFC4229]  */
    `Accept-Features`,

    /** "[RFC7231, Section 5.3.5]"  */
    `Accept-Language`,

    /** [RFC5789]  */
    `Accept-Patch`,

    /** [https://www.w3.org/TR/ldp/]  */
    `Accept-Post`,

    /** "[RFC7233, Section 2.3]"  */
    `Accept-Ranges`,

    /** "[RFC7234, Section 5.1]"  */
    Age,

    /** "[RFC7231, Section 7.4.1]"  */
    Allow,

    /** "[RFC7639, Section 2]"  */
    ALPN,

    /** [RFC7838]  */
    `Alt-Svc`,

    /** [RFC7838]  */
    `Alt-Used`,

    /** [RFC4229]  */
    Alternates,

    /** [RFC4437]  */
    `Apply-To-Redirect-Ref`,

    /** "[RFC8053, Section 4]"  */
    `Authentication-Control`,

    /** "[RFC7615, Section 3]"  */
    `Authentication-Info`,

    /** "[RFC7235, Section 4.2]"  */
    Authorization,

    /** [RFC4229]  */
    `C-Ext`,

    /** [RFC4229]  */
    `C-Man`,

    /** [RFC4229]  */
    `C-Opt`,

    /** [RFC4229]  */
    `C-PEP`,

    /** [RFC4229]  */
    `C-PEP-Info`,

    /** "[RFC7234, Section 5.2]"  */
    `Cache-Control`,

    /** "[RFC7809, Section 7.1]"  */
    `CalDAV-Timezones`,

    /** "[RFC7230, Section 8.1]"  */
    Close,

    /** "[RFC7230, Section 6.1]"  */
    Connection,

    /** [RFC2068][RFC2616]  */
    `Content-Base`,

    /** [RFC6266]  */
    `Content-Disposition`,

    /** "[RFC7231, Section 3.1.2.2]"  */
    `Content-Encoding`,

    /** [RFC4229]  */
    `Content-ID`,

    /** "[RFC7231, Section 3.1.3.2]"  */
    `Content-Language`,

    /** "[RFC7230, Section 3.3.2]"  */
    `Content-Length`,

    /** "[RFC7231, Section 3.1.4.2]"  */
    `Content-Location`,

    /** [RFC4229]  */
    `Content-MD5`,

    /** "[RFC7233, Section 4.2]"  */
    `Content-Range`,

    /** [RFC4229]  */
    `Content-Script-Type`,

    /** [RFC4229]  */
    `Content-Style-Type`,

    /** "[RFC7231, Section 3.1.1.5]"  */
    `Content-Type`,

    /** [RFC4229]  */
    `Content-Version`,

    /** [RFC6265]  */
    Cookie,

    /** [RFC2965][RFC6265]  */
    Cookie2,

    /** [RFC5323]  */
    DASL,

    /** [RFC4918]  */
    DAV,

    /** "[RFC7231, Section 7.1.1.2]"  */
    Date,

    /** [RFC4229]  */
    `Default-Style`,

    /** [RFC4229]  */
    `Delta-Base`,

    /** [RFC4918]  */
    Depth,

    /** [RFC4229]  */
    `Derived-From`,

    /** [RFC4918]  */
    Destination,

    /** [RFC4229]  */
    `Differential-ID`,

    /** [RFC4229]  */
    Digest,

    /** "[RFC7232, Section 2.3]"  */
    ETag,

    /** "[RFC7231, Section 5.1.1]"  */
    Expect,

    /** "[RFC7234, Section 5.3]"  */
    Expires,

    /** [RFC4229]  */
    Ext,

    /** [RFC7239]  */
    Forwarded,

    /** "[RFC7231, Section 5.5.1]"  */
    From,

    /** [RFC4229]  */
    GetProfile,

    /** "[RFC7486, Section 6.1.1]"  */
    Hobareg,

    /** "[RFC7230, Section 5.4]"  */
    Host,

    /** "[RFC7540, Section 3.2.1]"  */
    `HTTP2-Settings`,

    /** [RFC4229]  */
    IM,

    /** [RFC4918]  */
    If,

    /** "[RFC7232, Section 3.1]"  */
    `If-Match`,

    /** "[RFC7232, Section 3.3]"  */
    `If-Modified-Since`,

    /** "[RFC7232, Section 3.2]"  */
    `If-None-Match`,

    /** "[RFC7233, Section 3.2]"  */
    `If-Range`,

    /** [RFC6638]  */
    `If-Schedule-Tag-Match`,

    /** "[RFC7232, Section 3.4]"  */
    `If-Unmodified-Since`,

    /** [RFC4229]  */
    `Keep-Alive`,

    /** [RFC4229]  */
    Label,

    /** "[RFC7232, Section 2.2]"  */
    `Last-Modified`,

    /** [RFC-nottingham-rfc5988bis-08]  */
    Link,

    /** "[RFC7231, Section 7.1.2]"  */
    Location,

    /** [RFC4918]  */
    `Lock-Token`,

    /** [RFC4229]  */
    Man,

    /** "[RFC7231, Section 5.1.2]"  */
    `Max-Forwards`,

    /** [RFC7089]  */
    `Memento-Datetime`,

    /** [RFC4229]  */
    Meter,

    /** "[RFC7231, Appendix A.1]"  */
    `MIME-Version`,

    /** [RFC4229]  */
    Negotiate,

    /** [RFC4229]  */
    Opt,

    /** "[RFC8053, Section 3]"  */
    `Optional-WWW-Authenticate`,

    /** [RFC4229]  */
    `Ordering-Type`,

    /** [RFC6454]  */
    Origin,

    /** [RFC4918]  */
    Overwrite,

    /** [RFC4229]  */
    P3P,

    /** [RFC4229]  */
    PEP,

    /** [RFC4229]  */
    `PICS-Label`,

    /** [RFC4229]  */
    `Pep-Info`,

    /** [RFC4229]  */
    Position,

    /** "[RFC7234, Section 5.4]"  */
    Pragma,

    /** [RFC7240]  */
    Prefer,

    /** [RFC7240]  */
    `Preference-Applied`,

    /** [RFC4229]  */
    ProfileObject,

    /** [RFC4229]  */
    Protocol,

    /** [RFC4229]  */
    `Protocol-Info`,

    /** [RFC4229]  */
    `Protocol-Query`,

    /** [RFC4229]  */
    `Protocol-Request`,

    /** "[RFC7235, Section 4.3]"  */
    `Proxy-Authenticate`,

    /** "[RFC7615, Section 4]"  */
    `Proxy-Authentication-Info`,

    /** "[RFC7235, Section 4.4]"  */
    `Proxy-Authorization`,

    /** [RFC4229]  */
    `Proxy-Features`,

    /** [RFC4229]  */
    `Proxy-Instruction`,

    /** [RFC4229]  */
    Public,

    /** [RFC7469]  */
    `Public-Key-Pins`,

    /** [RFC7469]  */
    `Public-Key-Pins-Report-Only`,

    /** "[RFC7233, Section 3.1]"  */
    Range,

    /** [RFC4437]  */
    `Redirect-Ref`,

    /** "[RFC7231, Section 5.5.2]"  */
    Referer,

    /** "[RFC7231, Section 7.1.3]"  */
    `Retry-After`,

    /** [RFC4229]  */
    Safe,

    /** [RFC6638]  */
    `Schedule-Reply`,

    /** [RFC6638]  */
    `Schedule-Tag`,

    /** [RFC6455]  */
    `Sec-WebSocket-Accept`,

    /** [RFC6455]  */
    `Sec-WebSocket-Extensions`,

    /** [RFC6455]  */
    `Sec-WebSocket-Key`,

    /** [RFC6455]  */
    `Sec-WebSocket-Protocol`,

    /** [RFC6455]  */
    `Sec-WebSocket-Version`,

    /** [RFC4229]  */
    `Security-Scheme`,

    /** "[RFC7231, Section 7.4.2]"  */
    Server,

    /** [RFC6265]  */
    `Set-Cookie`,

    /** [RFC2965][RFC6265]  */
    `Set-Cookie2`,

    /** [RFC4229]  */
    SetProfile,

    /** [RFC5023]  */
    SLUG,

    /** [RFC4229]  */
    SoapAction,

    /** [RFC4229]  */
    `Status-URI`,

    /** [RFC6797]  */
    `Strict-Transport-Security`,

    /** [RFC4229]  */
    `Surrogate-Capability`,

    /** [RFC4229]  */
    `Surrogate-Control`,

    /** [RFC4229]  */
    TCN,

    /** "[RFC7230, Section 4.3]"  */
    TE,

    /** [RFC4918]  */
    Timeout,

    /** "[RFC8030, Section 5.4]"  */
    Topic,

    /** "[RFC7230, Section 4.4]"  */
    Trailer,

    /** "[RFC7230, Section 3.3.1]"  */
    `Transfer-Encoding`,

    /** "[RFC8030, Section 5.2]"  */
    TTL,

    /** "[RFC8030, Section 5.3]"  */
    Urgency,

    /** [RFC4229]  */
    URI,

    /** "[RFC7230, Section 6.7]"  */
    Upgrade,

    /** "[RFC7231, Section 5.5.3]"  */
    `User-Agent`,

    /** [RFC4229]  */
    `Variant-Vary`,

    /** "[RFC7231, Section 7.1.4]"  */
    Vary,

    /** "[RFC7230, Section 5.7.1]"  */
    Via,

    /** "[RFC7235, Section 4.1]"  */
    `WWW-Authenticate`,

    /** [RFC4229]  */
    `Want-Digest`,

    /** "[RFC7234, Section 5.5]"  */
    Warning,

    /** [https://fetch.spec.whatwg.org/#x-content-type-options-header]  */
    `X-Content-Type-Options`,

    /** [RFC7034]  */
    `X-Frame-Options`,

    /** [W3C Web Application Formats Working Group]  */
    `Access-Control`,

    /** [W3C Web Application Formats Working Group]  */
    `Access-Control-Allow-Credentials`,

    /** [W3C Web Application Formats Working Group]  */
    `Access-Control-Allow-Headers`,

    /** [W3C Web Application Formats Working Group]  */
    `Access-Control-Allow-Methods`,

    /** [W3C Web Application Formats Working Group]  */
    `Access-Control-Allow-Origin`,

    /** [W3C Web Application Formats Working Group]  */
    `Access-Control-Max-Age`,

    /** [W3C Web Application Formats Working Group]  */
    `Access-Control-Request-Method`,

    /** [W3C Web Application Formats Working Group]  */
    `Access-Control-Request-Headers`,

    /** [RFC4229]  */
    Compliance,

    /** [RFC4229]  */
    `Content-Transfer-Encoding`,

    /** [RFC4229]  */
    Cost,

    /** [RFC6017]  */
    `EDIINT-Features`,

    /** [RFC4229]  */
    `Message-ID`,

    /** [W3C Web Application Formats Working Group]  */
    `Method-Check`,

    /** [W3C Web Application Formats Working Group]  */
    `Method-Check-Expires`,

    /** [RFC4229]  */
    `Non-Compliance`,

    /** [RFC4229]  */
    Optional,

    /** [W3C Web Application Formats Working Group]  */
    `Referer-Root`,

    /** [RFC4229]  */
    `Resolution-Hint`,

    /** [RFC4229]  */
    `Resolver-Location`,

    /** [RFC4229]  */
    SubOK,

    /** [RFC4229]  */
    Subst,

    /** [RFC4229]  */
    Title,

    /** [RFC4229]  */
    `UA-Color`,

    /** [RFC4229]  */
    `UA-Media`,

    /** [RFC4229]  */
    `UA-Pixels`,

    /** [RFC4229]  */
    `UA-Resolution`,

    /** [RFC4229]  */
    `UA-Windowpixels`,

    /** [RFC4229]  */
    Version,

    /** [W3C Mobile Web Best Practices Working Group]  */
    `X-Device-Accept`,

    /** [W3C Mobile Web Best Practices Working Group]  */
    `X-Device-Accept-Charset`,

    /** [W3C Mobile Web Best Practices Working Group]  */
    `X-Device-Accept-Encoding`,

    /** [W3C Mobile Web Best Practices Working Group]  */
    `X-Device-Accept-Language`,

    /** [W3C Mobile Web Best Practices Working Group]  */
    `X-Device-User-Agent`,
    ;

}

