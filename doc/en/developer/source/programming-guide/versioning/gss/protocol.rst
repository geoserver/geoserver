.. _versioning_gss_protocol:

Core synchronization protocol
=============================

Protocol overview
-----------------

Central will contact each unit in a periodic fashion and synchronize all of the shared tables, each one according to its nature. The interval of synchronization will be different for each unit to take into account the different connectivity features of each installation (speed, delay, reliability).
For each unit and each layer the protocol will use two calls from central to the unit:

#) **(Central → Unit) GetCentralRevision(Layer)**. Central asks the Unit about the last Central revision that the Unit got differences to. Unit responds with the revision number.
#) **(Central → Unit) PostDiff(Layer, from, to, transaction)**. Central posts to Unit a synchronization document, the set of differences occurred on Layer between the last known revision number known to Unit and the current one. Unit compares that with the local modifications occurred since the last PostDiff call, finds out conflicts, applies only the non conflicting changes, stores the conflicts in the conflict table, marks down in a history table the new revision number, the list of ids of features under conflict, and the new central revision number
#) **(Central → Unit) GetDiff(Layer, from)**. Central asks for the differences occurred since the last known Unit revision number. Unit builds the difference skipping along the way all the commits incoming from Central as well as all the changes resulted in conflicts and returns the cleaned up difference as a synchronization document to Central. Central applies the differences and marks down the new Unit revision in a metadata table.

In case of connectivity failure the unit will be rescheduled for synchronization at a later time.
All the three types of tables can use the protocol. For some types of tables the differences in one of the two calls will be consistently empty.

Protocol restart
----------------

The network between Central and the Unit cannot be trusted to be reliable, thus the protocol need to take into account the need of partial synchronization restarts.

The first call, *GetCentralRevision*, is read only on both sides, so repeating it won't cause any issue.

The second call, *PostDiff*, is read only on the Central side and write only on the Unit side. Failure scenarios:

* If the connection drops whilst the synchronization document is being sent Unit won't perform any local change, thus the protocol can be restarted without problems.
* If Unit manages to commit the changes but the connection drops before it can return the OK, the protocol can be safely restarted from the beginning, it will simply result in multiple updates accumulating on the Unit side

The third call, *GetDiff*, is read only on the Unit side and write only on the Central side. Failure scenarios:

* If the connection drops while the request is being sent the protocol can be restarted from the beginning, or be restarted from GetDiff itself, without issues
* If the connection drops while the synchronization document is being returned the server side won't commit the changes and, again, the protocol can be restarted either from GetDiff or from the beginning.

Synchronization document
------------------------
A synchronization document is an XML document stating the layer being synchronized, the revisions at which the difference starts and ends, and the difference itself, expressed as a WFS 1.0 transaction summarizing all the changes occurred:

.. code-block:: xml
   
	<gs:Synchronization typeName=”parcels” fromRevision=”10” toRevision=”12”>
	   <wfs:Transaction>
	       <wfs:Insert>
	           …
	       </wfs:Insert>
	       ….
	   </wfs:Transaction>
	<gs:Synchronization>
	
Computing the synchronization document requires computing all the changes accumulated since the last successful synchronization, but it requires skipping the changes that occurred in it, since those are changes coming from the other side, that the other side is already aware of. 

On the server side this is easy, it is sufficient to record the last revision at which the changes coming from a Unit in a particular layer occurred, and then skip them when generating the differences for the Unit. By virtue of the protocol structure, there will be at most one to skip (since the changes coming from the Unit are at the end of the synchronization).

On the client side it's harder, as there is no way to guarantee there will just be one PostDiff in the queue. If a PostDiff succeeds on the Unit but the message fails to go back to Central (timeout, network failure) the latter will issue another PostDiff some time after that, which could contain more changes. So on the Unit side more synchronizations and local changes can intermingle.

This is the reason why Unit records all successful PostDiff calls, and it will use that record to create a synchronization document that contains only the changes occurred locally up to the last successful PostDiff.

The latter requires an explanation: why only up to the last successful synchronization? Because the uDig client, operating in parallel, might have lined up more changes between the last PostDiff and GetDiff, meaning some of them might introduce conflicts. Those will be taken into consideration only after being compared with a new PostDiff from the server.

Network communications
----------------------

All of the communications between Central and the various Unit will happen following an OGC style: HTTP protocol using GET and POST requests, HTTP basic authentication, and exchange of XML documents between the two sides.

Protocol granularity and concurrency
-------------------------------------

The synchronization protocol is layer based, meaning the number of communications required to fully synchronize a unit is three times the number of layers to be synchronized, each time paying a network delay.

The choice of layer granularity is taken in order to avoid exchanging and parsing big XML documents, as well as avoiding dealing with big amounts of data during the conflict computation (both are memory bound at the moment).

In order to work properly the protocol cannot update in parallel the same layer from two different units (thus the communication must be driven by Central as opposed as from the units, the latter would require locking). It's however possible to synchronize two layers in parallel, either from the same unit or from two different units, as the updates on those would be independent of each other.

The first implementation will use a simple linear scan, but a more sophisticated approach can be taken in the future to reduce the time needed to perform a synchronization and better use the available Central network capacity.