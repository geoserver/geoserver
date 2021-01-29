[description]
Configures the ByteBufferPool used by ServerConnectors.

[xml]
etc/jetty-bytebufferpool.xml

[ini-template]
### Server ByteBufferPool Configuration
## Minimum capacity to pool ByteBuffers
#jetty.byteBufferPool.minCapacity=0

## Maximum capacity to pool ByteBuffers
#jetty.byteBufferPool.maxCapacity=65536

## Capacity factor
#jetty.byteBufferPool.factor=1024

## Maximum queue length for each bucket (-1 for unbounded)
#jetty.byteBufferPool.maxQueueLength=-1

## Maximum heap memory retainable by the pool (-1 for unlimited)
#jetty.byteBufferPool.maxHeapMemory=-1

## Maximum direct memory retainable by the pool (-1 for unlimited)
#jetty.byteBufferPool.maxDirectMemory=-1
