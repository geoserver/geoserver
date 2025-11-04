[description]
Configures the ByteBufferPool used by ServerConnectors.
The bucket sizes increase linearly.
Use module "bytebufferpool-quadratic" for a pool that holds more coarse sized buffers.

[depends]
logging

[xml]
etc/jetty-bytebufferpool.xml

[ini-template]
## Minimum capacity of a single ByteBuffer.
#jetty.byteBufferPool.minCapacity=0

## Maximum capacity of a single ByteBuffer.
## Requests for ByteBuffers larger than this value results
## in the ByteBuffer being allocated but not pooled.
#jetty.byteBufferPool.maxCapacity=65536

## Bucket capacity factor.
## ByteBuffers are allocated out of buckets that have
## a capacity that is multiple of this factor.
#jetty.byteBufferPool.factor=4096

## Maximum size for each bucket (-1 for unbounded).
#jetty.byteBufferPool.maxBucketSize=-1

## Maximum heap memory held idle by the pool (0 for heuristic, -1 for unlimited).
#jetty.byteBufferPool.maxHeapMemory=0

## Maximum direct memory held idle by the pool (0 for heuristic, -1 for unlimited).
#jetty.byteBufferPool.maxDirectMemory=0

## Whether statistics are enabled.
#jetty.byteBufferPool.statisticsEnabled=false
