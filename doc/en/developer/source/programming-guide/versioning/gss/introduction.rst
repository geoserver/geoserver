.. _versioning_gss_introduction:

Introduction
============

The GSS (GeoServer Synchronization Service) module has been developed to support a geographic distributed versioning system in a proof of concept for a larger data distribution and editing project.
As such it proves feasibility of the distributed version control system, but does not provide any friendly tooling and is setup with strong limitations in mind:

* each node data is edited directly via a customized uDig that can handle and resolve conflicts. No extensions to the existing WFS or versioned WFS module has been created to handle conflicts, they are to be solved directly on the PostGIS database (to which the customized uDig connects directly)
* the setup requires a central node that orchestrates the synchronization, there is no peer to peer support
* there is no support for databases other than PostGIS, and even that case is limited to tables that have no referential integrity

General system structure
-------------------------
The GSS module goal is to keep in sync a distributed network of GeoServer instances, each using the same layers and each storing the layers in a PostGIS versioning data store.

Each node in the network is using a versioning PostGIS datastore, it has its own history and revision numbering, however the changes occurred on one node can propagate to all the other nodes (see Layers for the propagation rules).

The network is composed of three types of nodes:

* **unit**: it's a generic node where data modifications take place
* **central**: it's the central node that performs all the synchronization based on a predetermined schedule. No one can directly edit on central, every change gets pulled in from units instead.
* **administration unit**: a special unit that can edit administration tables that will drive central
 
The GSS module will be a single one, it will be configured to play the necessary role on each node.

Central polls all of the units in a round robin fashion, grabs changes from them and pushes the changes that it got from the other units to the remote unit. This means central has knowledge of all the units available in the network. It also means that, due to synchronization lag, a unit own database contents won't be the same as the central one, it will miss some modifications made by other units and will have some local modifications that have not made into the central database yet.

This may result in conflicting changes. See **Conflicts** for details about the nature and handling of a conflict.

Each unit will be equipped with a customized uDig that will connect directly to PostGIS using a versioning PostGIS data store. This client will understand the contents of the administration tables of the node (in particular, the conflicts one) and enforce special editing. This means, in particular, that the GeoServer running in each unit will have no other services running other than GSS itself.

Layers
------

The system handles three types of layers:

* **Published layers**: published layers can be edited only by the administration unit, every other unit receives changes from central but cannot perform local ones. It's a publish-subscribe model from central to the non administration units. The publish only nature of the layer will be enforced by the uDig client, that won't allow the layer to be edited.
* **Backup layers**: backup layers can be edited by every unit, but are setup so that each unit has its own records that no other unit can modify, and vice versa (by using a unique identifier that labels the record as such). In this case we have a publish-subscribe model from units to central, where central acts as a overview and backup mechanism. 
* **Synchronized layers**:  these layers can be edited by all remote units at the same time and thus changes to features will be traded in both directions.

Examples of published layers:

* base layers prepared by the administration unit

Examples of backup layers:

* conflict tables

Examples of synchronized layers:

* All shared layers that are supposed to be edited by the units and that have competency overlap between the units

Conflicts
----------

A conflict arises when a change on a feature goes both ways during a synchronization, that is, when central is trying to push a change on a feature that has been also modified in the unit since the last successful synchronization. In that case the change coming from central is applied and the unit local one is registered in a special conflict table that the client will use to show the user the differences in changes and allow the user to act on it.

In case of a conflict the user can either:

* accept the change coming from central thus ignoring the local one
* force the local change to overwrite the one coming from central 
* eventually merge the two, accepting parts of each. 

This results in a new modification to the feature that will either flow back to central during the next synchronization or cause a new conflict, in case another modification is coming down from central.

The uDig client will interpret the contents of the local **CONFLICTS** table and won't allow direct editing of a feature marked as in conflict until the conflict is resolved. The client will provide the user with tools to identify and fix the conflicts interactively.

Connectivity map
----------------

The connectivity map shows the geographic area associated to each unit with a color coding expressing the freshness of the last successful synchronization. It will be generated starting from housekeeping information that Central keeps up to date each time a synchronization occurs.

.. note:: The current implementation does not provide a connectivity map, though it is not hard to add one

GeoServer configuration
-----------------------

Each unit GeoServer will be locked down so that no actual service, besides GSS itself, will be available. 
The GSS communications will be using a predetermined user-name and password with no integration with the eventual user table.

Known limitations
------------------

**This design assumes there will not be any layer creation to be propagated and that there will be no layer structure changes either**.
If those needs arise the tables will have to be created off line on the entire network before starting to use them, and database structural changes will have to be performed manually with the synchronization protocol put on hold for the entire operation.

The GeoTools data store model upon all this work is based is simply not referential integrity aware, cannot deal with linked tables. As a result no attempt will be made to handle data with referential integrity constraints (the store can be modified to support referential integrity, but that will require a complete overhaul of the storage structures and code accessing them).

Table structures will be modified when they are version enabled (in particular, primary key will change) so existing foreign keys will loose their meaning and/or cause the system to misbehave (it is possible to change the structure of the versioning tables so that the original table is left untouched, that will require a complete overhaul of the storage structures and code accessing them).