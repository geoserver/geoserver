# control-flow

> **Note:** this document was prepared in April 2026 against the module behavior at that time. It is intended as
> onboarding material and is not actively maintained alongside the code. Treat it as a starting point: the module may
> have evolved since, and individual statements below may be out of sync with the current source. When in doubt,
> trust the code.

Throttles concurrent OWS requests and optionally rate-limits or prioritises them by user, IP or HTTP header.

The extension also ships a small IP blacklist filter that rejects blacklisted addresses before the dispatcher runs.

This README covers the internals - for configuration reference, see the GeoServer user manual.

## How it works

Every HTTP request that reaches GeoServer goes through a Spring-delegated servlet filter. For OWS requests the
dispatcher also fires callbacks once the request has been parsed into a service+operation. `ControlFlowCallback` is
registered as both a filter and a dispatcher callback, hooking both points from a single class. When the dispatcher
callback fires it asks a `FlowControllerProvider` for the list of controllers that apply to the request, walks that list
in priority order calling `requestIncoming(request, timeout)` on each, and only lets the operation run after every controller
has granted a slot. When the operation finishes the list is walked in reverse and each slot is released. Everything
else in this module is configuration surface around that core flow.

Rules live in `$GEOSERVER_DATA_DIR/controlflow.properties`. Example:

```properties
timeout=10
ows.global=16
ows.wms.getmap=4
user.ows.wms.getmap=6/s;2s
ip=8
ip.blacklist=192.168.1.8, 192.168.1.10
ows.priority.http=gs-priority,0
```

## Where the code lives

```
org.geoserver.flow              ControlFlowCallback (filter + DispatcherCallback in one class),
                                ControlFlowConfigurator, FlowController, FlowControllerProvider,
                                DefaultFlowControllerProvider, ControllerPriorityComparator, ControlModuleStatus
org.geoserver.flow.config       DefaultControlFlowConfigurator - the controlflow.properties parser
org.geoserver.flow.controller   FlowController implementations + ThreadBlocker implementations +
                                key/priority providers + IpBlacklistFilter + request matchers
```

Start with `ControlFlowCallback.java` - the rest of the module is easier to follow afterwards.

## Spring wiring (`applicationContext.xml`)

Three beans:

| Bean                   | Class                 | Role |
| ---------------------- | --------------------- | ---- |
| `controlFlowCallback`  | `ControlFlowCallback` | Implements **both** `AbstractDispatcherCallback` **and** `GeoServerFilter`. Acquires/releases controllers per request. |
| `ipBlacklistFilter`    | `IpBlacklistFilter`   | Rejects requests from blacklisted IPs with HTTP 403. Reads the same `controlflow.properties`. |
| `controlExtension`     | `ControlModuleStatus` | Shows up in the module status panel on the admin UI. |

Both `ControlFlowCallback` and `IpBlacklistFilter` are auto-discovered by GeoServer's `SpringDelegatingFilter`, which wraps every incoming
request in all `GeoServerFilter` beans it finds in the context. No `web.xml` changes, no explicit filter mapping.

If the data dir doesn't ship a custom `ControlFlowConfigurator` or `FlowControllerProvider`, `ControlFlowCallback.setApplicationContext` registers
the defaults (`DefaultControlFlowConfigurator`, `DefaultFlowControllerProvider`) as singletons on first access. See `ControlFlowCallback.registDefaultBeansIfNeeded`.

## High-level architecture

```mermaid
graph TD
    A[HTTP request] --> B[GeoServer filter chain]
    B --> C[IpBlacklistFilter]
    C -->|403| Z1[rejected]
    C --> D[ControlFlowCallback.doFilter]
    D --> E[Spring Dispatcher]
    E --> F[Dispatcher.operationDispatched]
    F --> G[ControlFlowCallback.operationDispatched]
    G --> H[FlowControllerProvider.getFlowControllers]
    H --> I[FlowController list<br/>sorted by priority]
    I --> J[for each controller:<br/>requestIncoming within remaining timeout]
    J -->|timeout| Z2[HTTP 503<br/>Requested timeout out...]
    J --> K[operation runs<br/>GetMap, GetCapabilities, ...]
    K --> L[Dispatcher.finished]
    L --> M[ControlFlowCallback.finished]
    M --> N[releaseControllers reverse order]
    N --> O[response]
    D -.->|finally safety net| N
```

## Per-request lifecycle

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant F as ControlFlowCallback<br/>(as Filter)
    participant D as GeoServer Dispatcher
    participant CB as ControlFlowCallback<br/>(as DispatcherCallback)
    participant P as FlowControllerProvider
    participant FC as FlowController[]
    participant OP as OWS operation

    C->>F: HTTP request
    F->>D: chain.doFilter
    D->>CB: operationDispatched(req, op)
    CB->>CB: REQUEST_CONTROLLERS.get() == null<br/>(not a nested dispatch)
    CB->>P: getFlowControllers(req')
    P-->>CB: List<FlowController> sorted by priority
    CB->>CB: REQUEST_CONTROLLERS.set(context)<br/>blockedRequests++
    loop each controller in priority order
        CB->>FC: requestIncoming(req', remainingTimeout)
        alt returned true
            FC-->>CB: granted
        else timeout (returned false)
            FC-->>CB: false
            CB-->>D: throw HttpErrorCodeException(503)
        end
    end
    CB->>CB: blockedRequests--<br/>runningRequests++
    CB-->>D: return
    D->>OP: run operation (render, query, ...)
    OP-->>D: result
    D->>CB: finished(req)
    CB->>CB: releaseControllers(false)<br/>nestingLevel--
    loop each controller, reverse order
        CB->>FC: requestComplete(req')
    end
    CB->>CB: runningRequests--<br/>REQUEST_CONTROLLERS.remove()
    D-->>F: response
    F->>F: finally releaseControllers(true)<br/>(precaution in case finished() is not called by any reason)
    F-->>C: HTTP response + X-Control-flow-delay-ms header
```

## Per-request state

Everything per-request lives in two `ThreadLocal`s on `ControlFlowCallback`:

- `REQUEST_CONTROLLERS` holds a `CallbackContext`: the list of controllers acquired for this request, the timeout in
force, the `Request` clone we used as the queue key, and a `nestingLevel` counter.
- `FAILED_ON_FLOW_CONTROLLERS` is a single boolean: `true` if acquisition failed partway through, so that on release
we know not to decrement `runningRequests` for a request that never actually started running.

Two process-wide `AtomicLong` counters gives an overall view of the server's state: `blockedRequests` (requests currently
sitting in some controller's queue) and `runningRequests` (requests that made it through all controllers and are now
executing).

### Re-entrance (integrated GWC, internal dispatches)

When GWC is integrated into the WMS pipeline - as transparent integration or as a native tile service - a single HTTP request
can re-enter `Dispatcher` on the same thread, which fires `operationDispatched` a second (and possibly third) time. Acquiring the same
controller twice on one thread would deadlock immediately on the first BasicOWS slot, so nested calls are detected via
`REQUEST_CONTROLLERS.get() != null` and only bump `nestingLevel`; actual release happens when `nestingLevel` hits zero. As a safety net, the
servlet-filter `doFilter` also calls `releaseControllers(true)` in its `finally` block, so if anything swallowed `finished()` on the way out the slot
is still released before the HTTP response returns.

## Controllers and blockers

Controllers live in `org.geoserver.flow.controller`. The `FlowController` contract is minimal:

```java
public interface FlowController {
    boolean requestIncoming(Request request, long timeout); // true if entered, false on timeout
    void    requestComplete(Request request);               // always called, matches every requestIncoming
    int     getPriority();                                  // used to sort via ControllerPriorityComparator
}
```

Controllers run in **priority order** on acquire and in **reverse** on release. The three base classes the rest build on:

- `SingleQueueFlowController` - a controller whose matching is a single `Predicate<Request>` and whose blocking behaviour is delegated to a `ThreadBlocker`.
The global and per-OWS controllers are instances of this.
- `QueueController` - for controllers that maintain a dynamic set of queues, one per key (IP, user). Creates `SingleQueueFlowController`-like queues on demand.
- `RateFlowController` - implements `FlowController` directly: no queue, only per-key counters over sliding time windows. Rate-limiter, not concurrency-limiter.

### ThreadBlocker implementations

A `ThreadBlocker` is the primitive that blocks a thread until a slot is available. Two exist:

- `SimpleThreadBlocker(queueSize)` - wraps a fair `ArrayBlockingQueue<Request>` of capacity `queueSize`. `requestIncoming` calls
`queue.offer(request, timeout, MS)` which returns `false` if the queue stayed full for the whole timeout; `requestComplete`
calls `queue.remove(request)`, matching by object identity against the `Request` clone stored in `CallbackContext`. `toString()` returns
`SimpleBlocker(N)`, which is the form that appears in the logs.
- `PriorityThreadBlocker(queueSize, priorityProvider)` - used only when `ows.priority.http=...` is configured. Maintains a `Set<Request>`
of currently-running requests and a `PriorityQueue<WaitToken>` of waiters, each waiter owning a `CountDownLatch`. When a slot frees,
`releaseNext()` polls the highest-priority waiter and counts its latch down. All mutations are under `synchronized(this)`.

### Controllers catalog

| Config key  | Class | What it does |
| ----------- | ----- | ------------ |
| `ows.global=N` | `GlobalFlowController` | `N` concurrent OWS operations across the whole server (the matcher always returns true). |
| `ows.<svc>[.<op>[.<fmt>]]=N` | `BasicOWSController` | `N` concurrent matches for the given `service[.request[.outputFormat]]` via `OWSRequestMatcher`. `toString()` is what produces `BasicOWSController(wms.getmap,SimpleBlocker(4))` in the logs. |
| `ip=N` | `IpFlowController` | Per-IP limit of `N` concurrent requests. Dynamic `ConcurrentHashMap<ip, Queue>` with 10s idle cleanup. Honours `X-Forwarded-For`. |
| `ip.<A.B.C.D>=N` | `SingleIpFlowController` | Hard-coded per-IP limit for a specific address. |
| `user=N` | `UserConcurrentFlowController` | Per-user concurrent limit, keyed by a `CookieKeyGenerator` (sets a `gs-user-id` cookie when missing). |
| `user.ows.<svc>[.<op>[.<fmt>]]=R/U[;Ds]` | `RateFlowController` | Sliding rate limit per cookie key - `R` requests per interval `U`, with an optional `Ds` delay before responding 429. |
| `ip.ows.<svc>[.<op>[.<fmt>]]=R/U[;Ds]`      | `RateFlowController`           | Same shape, keyed by IP via `IpKeyGenerator`. |
| `timeout=N`                                 | (not a controller)             | Seconds to wait in *any* queue before responding HTTP 503. Applied as a single shared deadline across all controllers for the request. |
| `ows.priority.http=Header,default`          | (config only)                  | Flips the blocker factory in `DefaultControlFlowConfigurator.buildBlocker` from `SimpleThreadBlocker` to `PriorityThreadBlocker` and reads the priority from `Header` (integer; `default` when missing/invalid). |
| `ip.blacklist=a,b,c` / `ip.whitelist=a,b,c` | `IpBlacklistFilter`            | Reject the listed IPs with HTTP 403 at the servlet-filter level before the dispatcher runs. Whitelist overrides blacklist. A `*` in a pattern becomes the regex `(.{0,1}[0-9]+.{0,1}){0,4}`. |

Rate unit in `R/U[;Ds]` is one of `s`, `m`, `h`, `d` (see the `RATE_PATTERN` constant).

### The parser

`DefaultControlFlowConfigurator.buildFlowControllers()` walks `controlflow.properties` and maps each key to a controller per
the table above. A `PropertyFileWatcher` detects on-disk changes; `DefaultFlowControllerProvider.checkConfiguration` rebuilds
the sorted controller list under `synchronized(configurator)` when `configurator.isStale()` returns `true`. `timeout` is
stored in milliseconds (key value × 1000) on a `volatile long`.

## Response headers

`ControlFlowCallback` sets the following response headers:

- `X-Control-flow-delay-ms` - milliseconds spent between entering `operationDispatched` and either clearing flow-controller
acquisition or throwing. Set in the `finally` block of `operationDispatched`, so every response carries it.
- `X-Concurrent-Limit` / `X-Concurrent-Requests` - set by controllers enforcing concurrency limits; useful for clients that want to self-throttle.

The 503 body reads `Requested timeout while waiting to be executed, please lower your request rate`.

## Log format cheatsheet

Messages emitted under the `geoserver.flow` logger:

| Level | Message | Meaning |
| ----- | ------- | ------- |
| INFO  | `Request [...] starting, processing through flow controllers` | A non-nested dispatch has started acquiring controllers. |
| FINE  | `Request [...] enter BasicOWSController(wms.getmap,SimpleBlocker(4))/4` | About to call `requestIncoming` on this controller. The trailing `/N` is the **live** queue size at log time, not a per-request counter. For a `SimpleThreadBlocker` of capacity 4, `/4` indicates the queue is full and the call will block until a slot frees or the timeout expires. |
| FINE  | `Request [...] exit BasicOWSController(...)/N` | `requestIncoming` returned `true`; the request is now counted in the queue. `/N` is the queue size **after** entry. Not emitted on timeout - there is no `exit` line when the controller returned `false`. |
| INFO  | `Request control-flow performed, running requests: X, blocked requests: Y` | Emitted in the `finally` of `operationDispatched`; controller acquisition has finished (successfully or by throwing). |
| INFO  | `releasing flow controllers for [...]` | Release path has started (`finished()` or the `doFilter` `finally`). |
| INFO  | `Request completed, running requests: X, blocked requests: Y` | End of release path. |
| FINE  | `Nested request found, not locking on it` | Re-entrant `operationDispatched` on the same thread; only `nestingLevel` was incremented. |
| WARN  | `Request [...] held flow-controller slot for <N>ms, over the configured <timeout>ms timeout; peer requests may have returned HTTP 503 while waiting for this one.` | Release-time stuck-slot warning. |

Because `/N` is a shared counter read at log-emission time, values across different threads' `enter`/`exit` lines fluctuate freely. Do not read them as belonging to a single request.

## Operational notes

- **Only queue waits are bounded.** `timeout` caps time spent in a queue, not execution time.
- **`timeout=0` or omitted disables the queue-wait deadline.** Waiters then block on `BlockingQueue.put` with no timeout. Not recommended
for production: a slow operation can cause waiters to pile up indefinitely.
- **`controlflow.properties` is hot-reloaded.** Changes are picked up on the next request through the `PropertyFileWatcher.isStale` check in
`DefaultFlowControllerProvider.checkConfiguration`. No restart required.
- **Ordering matters.** Lower-priority controllers are acquired first and released last. Reverse-order release drains the most specific
limits (usually OWS-level) before more general ones (global), which reduces the chance of a low-priority request keeping a slot a higher-priority request needs.
- **`ControlFlowCallback`'s constructor calls `REQUEST_CONTROLLERS.remove()`** to isolate unit tests from leftover thread-locals
in the test runner. At runtime there is a single callback bean, so the call is effectively a no-op.

## Developer tips

- Start with `ControlFlowCallback.java`, specifically `operationDispatched` and `releaseControllers`. The rest of the module is easier to follow afterwards.
- To add a new flow controller: implement `FlowController`, add a parser branch in `DefaultControlFlowConfigurator.buildFlowControllers`,
and provide a `toString()` that includes the controller's parameters - the logs rely on it.
- To add a new blocker: implement `ThreadBlocker` and wire it through `buildBlocker` in the configurator, or wrap it directly inside a controller.
- For concurrency tests use `AbstractFlowControllerTest` and `FlowControllerTestingThread`. These were recently hardened (`state` is `volatile`,
`waitBlockedOrTimedOut` accepts `TIMED_OUT`) to eliminate sporadic CI failures caused by ad-hoc thread-state polling. Reuse them rather than rolling your own.

