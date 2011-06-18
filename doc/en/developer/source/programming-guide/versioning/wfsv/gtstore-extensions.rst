.. _versioning_implementation_gtstore:

Versioning WFS - GT2 Datastore extensions
=========================================

Versioning requires some changes on the datastore interface.

Schema handling
---------------

Assuming that a vesioned datastore is able to handle both versioned and non versioned feature types and that it can make them change state, the following additions to FeatureType getSchema(String) and String[] getTypeNames() in order to chech wheter a type is versioned, and in order to transition it between the versioned and non versioned states::

  boolean isVersioned(String typename) throws IOException;
  void setVersioned(String typename) throws IOException;

setVersioned should be optional, throwing UnsupportedOperationException on datastore that do not provide it, and an IOException in the case where it does not make sense to version a feature type, such as the ChangeSet one, which is used to provide logging (see Versioning WFS - Database design).

Data gathering
--------------

Querying the datastore should take versioning into account.

The Query interface already provides a String getVersion which is WFS standard compliant. If no version is provided, the latest version will be assumed.

We'll use it, thought it would be nice to expand its semantics to take care of branches and time based extractions:

* "1024": extract revision 1024
* "projectX:1024": extract revision 1024 from branch "projectX"
* "20061203122500": time based extraction using ISO like format (December 12 2006, 12:25:00)
* "projectX:20061203122500": as above, with branch specification.

Data writing
------------

Here things get messier. Write side of the world does not have any notion of versioning, since Query is basically not used at all in the writing interfaces (nor DataStore, nor FeatureStore).
Moreover, here both version and user id shall be provided in order to perform the operation against a versioned datastore.

The best guess I have at the moment is to leverage the Transaction inteface, and in particular the Transaction properties, thus making revision we're working against and user id two "well known" transaction properties the versioned datastore should use.
If the two above are not available the versioned datastore can either fall back on some default user provided during datastore instantiation and last revision, or throw an exception.
