[description]
Configures the common HTTP parameters for all HTTP protocol versions.
The parameters describe the non-wire aspects of the HTTP semantic that
have a meaning for any HTTP protocol versions.

[tags]
server

[lib]
lib/jetty-http-${jetty.version}.jar
lib/jetty-util-${jetty.version}.jar

[xml]
etc/jetty-http-config.xml

[ini-template]
# tag::documentation[]
### Common HTTP configuration
## Scheme to use to build URIs for secure redirects
# jetty.httpConfig.secureScheme=https

## Port to use to build URIs for secure redirects
# jetty.httpConfig.securePort=8443

## Input buffer size (in bytes), used if not overridden by TLS or other concerns
# jetty.httpConfig.inputBufferSize=8192

## Response content buffer size (in bytes)
# jetty.httpConfig.outputBufferSize=32768

## Max response content write length that is buffered (in bytes)
# jetty.httpConfig.outputAggregationSize=8192

## If HTTP/1.x persistent connections should be enabled
# jetty.httpConfig.persistentConnectionsEnabled=true

## Max request headers size (in bytes)
# jetty.httpConfig.requestHeaderSize=8192

## Default response headers size (in bytes)
# jetty.httpConfig.responseHeaderSize=8192

## Max response headers size (in bytes), or -1 to use jetty.httpConfig.responseHeaderSize as the max.
# jetty.httpConfig.maxResponseHeaderSize=16384

## Whether to send the Server: header
# jetty.httpConfig.sendServerVersion=true

## Whether to send the Date: header
# jetty.httpConfig.sendDateHeader=false

## Max per-connection header cache size (in nodes)
# jetty.httpConfig.headerCacheSize=1024

## Maximum number of error dispatches to prevent looping
# jetty.httpConfig.maxErrorDispatches=10

## Relative Redirect Locations allowed
# jetty.httpConfig.relativeRedirectAllowed=true

## Redirect body generated
# jetty.httpConfig.generateRedirectBody=false

## Whether to use direct ByteBuffers for reading or writing
# jetty.httpConfig.useInputDirectByteBuffers=true
# jetty.httpConfig.useOutputDirectByteBuffers=true

## The minimum space available in a retained input buffer before allocating a new one.
# jetty.httpConfig.minInputBufferSpace=1024
# end::documentation[]

# tag::documentation-compliance[]
## HTTP Compliance: STRICT, RFC9110, RFC7230, RFC7230_LEGACY, RFC2616, RFC2616_LEGACY, LEGACY
# jetty.httpConfig.compliance=RFC9110

## URI Compliance: DEFAULT, LEGACY, RFC3986, RFC3986_UNAMBIGUOUS, UNSAFE
# jetty.httpConfig.uriCompliance=DEFAULT

## URI Compliance: DEFAULT, LEGACY, RFC3986, RFC3986_UNAMBIGUOUS, UNSAFE
# jetty.httpConfig.redirectUriCompliance=DEFAULT

## Cookie compliance mode for parsing request Cookie headers: RFC6265_STRICT, RFC6265, RFC6265_LEGACY, RFC2965, RFC2965_LEGACY
# jetty.httpConfig.requestCookieCompliance=RFC6265

## Cookie compliance mode for generating response Set-Cookie: RFC2965, RFC6265
# jetty.httpConfig.responseCookieCompliance=RFC6265

## MultiPart compliance mode: RFC7578_STRICT, RFC7578, LEGACY
# jetty.httpConfig.multiPartCompliance=RFC7578
# end::documentation-compliance[]
