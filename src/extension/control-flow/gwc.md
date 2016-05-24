Control flow and the GWC integration issue
==========================================

This document collects ideas about what's making nested GWC request problematic to handle in the control-flow plug-in.

How Control-flow works
----------------------

Control-flow limits the amount of concurrent requests actually executing by tapping
into the request lifecycle and deciding if a request can progress, or has to wait,
during the request parsing step.

Each rule in control-flow generates a `FlowController` object, the ones limiting the
amount of concurrent requests use a `BlockingQueue` with a limited size, if the request
matches their condition the request is put in the blocking queue, and removed once the
request is completed. If a queue is full, the attempt will block, either for an infinite
time, or if a maximum time to wait is set, up to said time (after which an exception will
be thrown and the request discarded).

Given each controller is in fact a separate lock, some deadlock avoidance rules
have to be followed:
* All requests grab the locks in the same order
* A single request does not grab the same lock twice
* There is a guarantee the locks are going to be released, by removing the request
objects from the queues, at the end of the request.

For normal requests the above conditions are respected and no deadlock occurs. 
The GWC integration however challenges the above and can indeed cause issues.

GWC request integration
-----------------------

GWC integrates in GeoServer in two ways:
1. When a GWC tile oriented service is called, by creating a internal proxy request to the WMS whenever a tile is missing. Each request to the GWC service is still captured by the OWS Dispatcher under a fake "GWC" service.
2. When direct integration is enabled, a WMS request is intercepted, if it happens to match a tile in a GWC cached gridset, it's turned into an internal GWC request, and if the tile is missing, again a internal proxy request to the WMS is made.

Internal requests enter the Dispatcher just like an external request, they are simply injected by using a mock `HttpServletRequest` and `HttpServletResponse`. 
These requests are also working under a futher lock, the GWC metataline one, preventing multiple requests hitting the same missing meta-tile to compute it in parallel.
This causes issues as either the deadlock avoidance rules are violated, causing deadlock, or the control-flow limits are violated, creating a loophole by which the load can go beyond the limits set in the configuration.

Approaches leading to deadlock
==============================

No nested request treatment scenario
------------------------------------

Let's imagine there is no recognition whatsoever of the nested request and consider what happens in the two cases.

When a direct GWC tile service (e.g., WMTS) is invoked:
* The direct request comes in and it is interpreted as a GWC one, the global OWS and the GWC queues receive a token
* The tile is missing, a internal request is made
* The internal request hits the dispatcher again, and the global OWS flow controller is hit again, making the request take two slots in sequence. This causes deadloks, e.g., if globally there are only 2 slots and two concurrent requests manage to take one each, by the time the nested request is issued, they will deadlock.

When a WMS matching the transparent integration is invoked:
* The direct request is interpreted as a WMS one, the locks will be taken as usual
* GWC intercepts, finds the tile is matching a cached gridset, the tile is missing, a internal request is made
* The internal requests hits all the same locks again, causing deadlocks just like the scenario above (just with a higher likeliness)

Recognize nesting and avoid taking a second time the same lock
--------------------------------------------------------------

In this scenario the code is modified so that a request hitting the flow controllers again cannot grab them a second time (e.g., by recognizing the same thread is already holding a token in the blocking queues, or changing the mechanism, or using thread locals to mark the repeated grab... how it's done does not really matter).

In this scenario the direct GWC tile service can deadlock with a straight WMS request because the locks would be taken in a different order, e.g.
* T1, handling the GWC request, grabs the OWS global and GWC lock, exhausting the global queue
* In T1 GWC figures out the tile is missing and issues the internal request
* T2 handles the WMS request and manages to exhausts the queue of the WMS flow controller (which is processed first, since it has a queue with a smaller size)
* T1 does not lock on global again, but tries to push its request, failing, in the WMS queue, and locks there
* T2 holding the WMS lock tries to get its request in the global OWS queue, failing, and the two threads are now in deadlock

Release all locks before taking new ones
-----------------------------------------

In this scenario control-flow recognizes the request is nested and releases all slots acquired in the flow controllers
before trying to acquire new ones. 
Unfortunately this still causes deadlocks due to the metatiling lock in GWC.

In this scenario two direct GWC tile service requests on the same GWC metatile can deadlock:
* T1, in the outer request, grabs the slots
* In T1 GWC notices the tile is missing, grabs the metatile lock MTL1 and issues the internal WMS request
* In T1 control-flow releases the locks
* T2, in the outer request, grabs the slots and exhausts one of them (e.g., the global one)
* T1 tries to grab one slot in global flow controller, but fails and remains locked
* T2 also notices the tiles are missing, tries to lock on the metatile lock MTL1, and remains locked. 
* The two threads are now in deadlock 

Approaches not leading to deadlock
==================================

These approaches do not lead to deadlock on nested requests, but may have other downsides.

Recognize nesting and do not grab any locks in the nested request (current implementation)
------------------------------------------------------------------------------------------

In this scenario no deadlocks occur, but the limits are not respected for direct GWC tile service requests.

When a direct GWC tile service (e.g., WMTS) is invoked:
* The direct request comes in and it is interpreted as a GWC one, the global OWS and the GWC queues receive a token
* The tile is missing, a internal request is made
* The internal WMS request is recognized as nested, and control-flow does not try to grab any further lock. This means the nested GWC requests are not subject to the WMS limits anymore, thus these are only subject to the limit of how many GWC requests can run in parallel (as a further consideration, sometimes WMTS is setup as non caching at the layer level, for those layers all requests hit the internal WMS).

When a WMS matching the transparent integration is invoked:
* The direct request is interpreted as a WMS one, the locks will be taken as usual
* GWC intercepts, finds the tile is matching a cached gridset, the tile is missing, a internal request is made
* The nested request is recognized, no locks are taken. This is fine, since the outer one already grabbed the WMS locks.

Setup different queues for direct and nested requests
-----------------------------------------------------

In this scenario we assume control-flow recognizes the nested requests, but instead of trying to lock them
into the main flow controllers, they get locked into a second set of flow controllers, dedicated to nested requests.

This approach would require to change the `FlowControllerProvider#getFlowControler(Request r)` with an extra indication of which
group of flow controllers to create, e.g., `FlowControllerProvider#getFlowControler(Request r, String groupId)`. 
Given the module exposes this interface and allows other implementations, this would be a backwards incompatible change.

Give the outer request a special treatment
------------------------------------------

One could make the outer request be treated in a special way, e.g., only lock on the GWC limits but don't grab
any other lock. There are two issues with this approach:
* With direct integration it is hard to know if a WMS request will be treated a GWC cacheable tile (some coupling with GWC is necessary)
* The `FlowController` interface provides no means to check on what type of request the controller will match, thus it's not possible to tell if the controller would even be GWC specific, or not.




