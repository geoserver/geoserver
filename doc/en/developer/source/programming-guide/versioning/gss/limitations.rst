Known limitations
=================

The current implementation is  a proof of concept with these major objectives:

* prove that synchronization can be attained
* make it solid against network failures
* make it implementable in a short time

There are other important objectives that have been sacrificed in order to attain the above objectives.

Someone needs to create the tables to be synchronized manually on both ends, and then register them in the proper metadata tables. Would be nicer if Central could post a feature type description to the units and have the tables be created instead (obvious issues: mismatch between raw SQL types and what can be expressed in a XML schema).

The service has no support for modification of the table structure.

No support for foreign keys.

The service assumes the feature type schemas on both sides are the same, there are no checks.

At the moment there is some redundancy in the manual work of setting up the synchronized tables. Central could learn which tables the unit wants to synch up via a GetCapabilities of sort for example, and/or could push a list of tables to be synchronized 

The Central part has no service protocol, though it might be interesting to have a way to interact with it:

* get a list of units and layers (capabilities)
* informations about latest synchronization per unit or per layer
* run manually the synchronization per unit, per layer (as opposed to waiting the service to do it automatically)

The service obviously lack a GUI to setup layers to be synchronized on the unit side and units, layers and synchronization details (frequency, time windows) on the central side. At the moment everything has to be setup manually in the database.
The system requires usage of UUID primary keys in all synchronized tables. There is the issue of generating them for data loading purposes. PostgreSQL has an installable module that allows for the generation of such keys1, otherwise the table can be setup in PostGIS using manual SQL and then it can be populated using a script leveraging versioning PostGIS datastore to insert the data (even before version enabling the table). 