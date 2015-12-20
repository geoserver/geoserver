.. _versioning_implementations_oracle:

Oracle Workspace Manager
==========================================

Workspace manager is an Oracle module that allows for version control like features while retaining the ability to work against the database as if the versioning system was not there.
This system is very powerful, but shows some notable differences from normal version control systems.

Workspace manager is a forward change with full copies system, just like ArcSDE, thought the implementation is quite different, and whilst not have a concept of revision, it still provides branches, tags and change dates. It has no attribution, so it's not possible to know who did perform a specific change.

The main versioning concepts in Workspace manager are workspaces (branches) and savepoints (tags). Each workspace can have zero or more child workspaces, and zero or more parent workspaces (multiparent workspaces have been introduced in Oracle 10g).

Workspace aware client applications do select which workspace they do work in, and then operate against the database as usual, with the notable difference that each DML statement does in fact create new rows in the versioned table, so that fully history is recorded.

Merges can occur two ways:

* from parent to child workspace: these are called "refreshes", and can be manual, or set as automatic (continually refreshed workspaces), so that each change on the parent workspace is directly reflected on child workspaces (not sure how conflicts are handled should they arise, the guide does not tell).
* From child to parent: this is called merge. A merge can also close the child workspace (it's an option).

Once work is done on a workspace the latter can be deleted, this implies the deletion of all information about that workspace too.

Save points are a way to establish tags on a workspace. They are used for a few purposes:

* to mark an interesting state of the workspace that can be consulted aftewards. It is possible to "enter" a save point afterwards and have a read only view of the database.
* to mark a position and eventually revert to its state. A rollback to a save point will remove all changes that have been performed after the save point, without any possibility to undo the rollback (so it removes the intermediate changes from the database, it does not create new changes that set the state back to the savepoint like a svn like rollback);
* to mark the start of a new child workspace (much like in CVS branching best practices).

An extra set of lock operations is provided, besides the usual lock primitives, that allow inter workspace locking, allowing a row to be locked in both child and parent workspace, limiting the likeliness of conflicts.

The versioning implementation is in fact quite complex, and it's not explained in any detail in Oracle documentation.

When a table is marked as versioned, a few things occur:

* the table gets renamed to <table>_lt, and a few new colums appear: version, createTime, retireTime, nextVer, delStatus, ltLock.
* A set of views is built to gather significant information about the versioned table.

First and foremost, a new view named <table> is created, so that the user has the impression that the original table is still there, and a set of trigger is created so that each insert/update/delete operation on top of the view really alters the versioned table. The view in turn selects only live rows associated to the current workspace, so that changing workspace or savepoint changes the rows returned by the view.
A slew of other views is built to enabled the creation of diffs, to query about parent/child conflicts or long lived locks.

In the WMSYS schema a lot of support tables is available that identifies association between version numbers and workspace and save point names, as well as parent child relations, support for global lock and the like.
