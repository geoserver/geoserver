.. _versioning_implementation_wfs_extensions:

WFS Extensions
============================

Versioned features support requires a protocol that knows about the extra features such as history and snapshot gathering, as well as conflicts and rollbacks.
Here we discuss an extension to the WFS protocol, although a simpler REST implementation would be appealing as well and deserves further inspection (JSON seems to be a popular choice for RESTful implementations). We're starting with WFS-T because we already have a solid base supporting it, so we an focus on what is new, instead of writing an entirely new API. But if there is consensus around a REST equivalent of WFS(-T) becomes we would be very interested in supporting it and extending it for versioning.

Authentication
--------------

It is advisable that calls to WFS use some kind of authentication in order to attribute each change to the actual user that performed it. Well, in fact even a plain WFS-T should be subjected to authentication, in order to avoid data vandalism.

In any case, the authentication process should be "out of band" with respect to the WFS protocol calls, for example using HTTP basic/digest authentication. Geocollaborator discussion includes some info about authentication libraries and schemes we could use.

It would be interesting to hear from client developers what authorization mechanisms they would be able to support.

WFS protocol extensions
-------------------------

The WFS protocol must be extended in order to support versioning, that is revision awarness, logs, differences.

Yet, WFS clients unaware of versioned WFS should be able to work against treating it like a plain WFS, just like Oracle does when an unaware client works against a versioned table:

* a plain GetFeature call must return the latest revision;
* Transaction, in absence of branches, works just like usual from the user point of view (but of course creates new versioned records instead of altering existing ones);
* Lock, in absence of branches, works the same as usual, too, so it's intended to work against the last revision (probably some changes are needed to handle actual record changes during the lock duration).

Besides that, real versioning requires both changes in the existing calls, and a new set of calls as well.

.. note:: Warning This poses a problem, because WFS request/response elements are defined in a XML schema. Extensions will be performed using XML schema standard mechanisms, that is, extension and type substitution. Were possible and sensible, WFS 1.0 extension points provided for vendor parameters will be used.

Versioning WFS will be realized on top of the Geoserver 1.6.x WFS module, which is designed priarily for WFS 1.1, but supports 1.0 as well. The following schema elements do extend WFS 1.1 schema, but we'll support WFS 1.0 as well. Extensions do not depend on specific WFS 1.1 features, thus will be applied the same way to both WFS protocols.
GetFeature

We obviosly need to be able and retrieve features at a specific revision. In WFS an hook is already available to support versioned request, in particular in the Query element of a GetFeature request:


.. code-block:: xml 

	<xsd:complexType name="QueryType">
	  ...
	  <xsd:attribute name="featureVersion" type="xsd:string" use="optional">
	    <xsd:annotation>
	      <xsd:documentation>
	        For systems that implement versioning, the featureVersion
	        attribute is used to specify which version of a particular
	        feature instance is to be retrieved.  A value of ALL means
	        that all versions should be retrieved.  An integer value
	        'i', means that the ith version should be retrieve if it
	        exists or the most recent version otherwise.
	      </xsd:documentation>
	    </xsd:annotation>
	  </xsd:attribute> 
	  ...

This would make us standard compliant with WFS and limit filtering capabilities to equality, thought we have to implement ALL as well. The documentation explicitly says either ALL or an integer must be used, but we'll be more lenient and support other formats as well:

* numeric version
* timestamp (in some ISO standard, locale independent format)
* branch:version
* branch:timestamp
* ALL
* FIRST (first state committed in a branch)
* LATEST (head of a branch). This does not pose validation issues, since version attribute is a generic "string". LATEST should become the default value for Query featureVersion attribute, that is, if not specified, the server acts as if the latest version is required.

Returned feature collections should provide a version number, so that clients can build a checkout and just ask for differences in subsequent requests. This can be done by extending FeatureCollection into a VersionedFeatureCollection, being part of the same substitution group as FeatureCollection, which reports the last version for each feature type (given that some feature types may not be versioned, and others may come from different versioning datastores):

.. code-block:: xml 

	<VersionedFeatureCollection ... >
	   <!-- collection made of features coming from feature types ft1, ft2, ft3, 
	        ft1 and ft2 coming from different versioning data stores, ft3 being unversioned -->
	   ...
	   <FeatureTypeVersions>
	      <FeatureTypeVersion typeName="ft1" featureVersion="1156"/>
	      <FeatureTypeVersion typeName="ft2" featureVersion="1256"/>
	      <!-- ft3 not included in this list because -->
	   </FeatureTypeVersions>
	</VersionedFeatureCollection>

Providing version numbers is an optimization that can be added later, since basic versioning functionality is there even without checkouts.

*Here we should add an extension of GetFeature that is able to return features modified by a specific user*

GetLog
------

Setting up an equivalent of "svn log", given our current infrastructure, could be done by simply exposing the ChangeSet table as a read only feature type, since it has a bbox attribute which is a geometry.
There are a few drawbacks though:

* it's not possible to get a log for a specific feature type (since feature types modified in a tranaction are stored in an associated transaction)
* the output format is not designed for readability
* it's not possible to use all the formats for the version (time based, for example)

So, a new call is required, that mimics GetFeature, but allows to specify in the Query a starting and ending feature version. This can be done by extending Query into a DifferenceQuery that supports a fromFeatureVersion attribute, and creating the log operation accordingly (by copy and paste from GetFeature, since complex type restrictions are not widely supported by parsers).

.. code-block:: xml 

   <xsd:complexType name="GetLogType">
      <xsd:complexContent>
         <xsd:extension base="wfs:BaseRequestType">
            <xsd:sequence>
               <xsd:element ref="wfsv:DifferenceQuery" maxOccurs="unbounded"/>
            </xsd:sequence>
            <xsd:attribute name="resultType"
                           type="wfs:ResultTypeType" use="optional"
                           default="results">
            </xsd:attribute>
            <xsd:attribute name="outputFormat"
                           type="xsd:string" use="optional"
                           default="text/xml; subtype=gml/3.1.1">
            <xsd:attribute name="maxFeatures"
                           type="xsd:positiveInteger" use="optional">
         </xsd:extension>
      </xsd:complexContent>
   </xsd:complexType>
   <xsd:complexType name="DifferenceQueryType">
      <xsd:sequence>
        <xsd:element ref="ogc:Filter" minOccurs="0" maxOccurs="1"/>
      </xsd:sequence>
      <xsd:attribute name="typeName" type="xsd:QName" use="required"/>
      <xsd:attribute name="fromFeatureVersion" type="xsd:string" default="FIRST"/>
      <xsd:attribute name="toFeatureVersion" type="xsd:string" use="optional" default="LAST"/>
   </xsd:complexType>

GetLog has basically the same schema as GetFeatures. Having an output format choice is important to have a variety of clients and display use it (see next paragraph). Having a "maxFeatures" attributes allows to limit the number of log entries returned, and closely mimics svn log --limit xxx. resultType allows to cheaply count how many log entries are there (not sure this is necessary, may well be removed).

Query has been replaced by DifferenceQuery, a versioned companion with two differences compared to the standard Query:

* Does not have a property list, since it's meant for extracting diffs and logs from a feature type, not extracting actual features.
* Has starting and ending version.

The default GetFeature output format is GML, which is good for WFS clients to map, but hard for a human being to read. I guess a client may want to show the output in html or pure text, because in this respect the call would be more similar to a WMS GetFeatureInfo call. So, GetLog will provide multiple representations just like GetFeature, and support both GML and a human readable format.

GetDiff
-------

An equivalent of svn diff could be interesting because it would allow to:

* Pinpoint what changed between two versions on the attribute level.
* Perform rollbacks, just gather a reverse diff from the server and use it to build a transaction call.

A GetDiff call would really look like a GetFeature call, but using DifferenceQuery instead of a plain Query:

.. code-block:: xml 

   <xsd:complexType name="GetDiffType">
      <xsd:complexContent>
         <xsd:extension base="wfs:BaseRequestType">
            <xsd:sequence>
               <xsd:element ref="wfsv:DifferenceQuery" maxOccurs="unbounded"/>
            </xsd:sequence>
            <xsd:attribute name="user" type="xsd:string" use="optional"/>
            <xsd:attribute name="outputFormat"
                           type="xsd:string" use="optional"
                           default="application/xml; subtype=wfsv-transaction/1.1.0">
            </xsd:attribute>
         </xsd:extension>
      </xsd:complexContent>
   </xsd:complexType>

The default result would be a Transaction call that can be applied on the server to merge the diff on another branch (when branches will be supported) or to perform a rollback. The standard (unversioned) transaction call is especially well suited, because it allows for specification of what needs to be created, deleted and udpated in order to update data to a specific revision, so it's suggested to be the default output format.
To simplify things for clients, only fid filters will be added in the resulting Transaction call (to avoid having light clients to implement a full OGC Filter handling).

Rollback
--------

This operation is not really required if GetDiff is implemented, since GetDiff + Transaction could be used, but:

* The difference maybe be very big, creating issues on unreliable or slow networks, since the diff must travel the network twice (first as a GetDiff result, then as a Transaction call).
* As the SQL call sampler shows, a direct server side rollback can be a lot more efficient.

Since both of these can be thought as optimizations, this could be thought as an optional feature that only advanced Versioning WFS implementations do support.

A rollback call should identify which changes should be rolled back, so in principle it requires the revision at which we want to roll back, and a commit message. This can be implemented as a new element type in transaction (RollbackElementType), and is further discussed in the Transaction paragraph.

Merge
------

The same considerations made for rollback can be made for a cross branch merge. Since the first implementation does not consider branches, merge won't be discussed further.

Transaction
-----------

Transaction modifications for versioning need to consider that a server will probably serve both versioned and non versioned feature types, possibly have multiple versioned datastores, and that clients using the plain WFS protocol should be allowed to participate and work against a versioned WFS too, so modifications need to be minor, and optional. In particular, there is a need for:

* a place where commit message can be specified (optional).
* returning the new revision information.
* handling rollbacks and merges
* handling conflicts, at least against clients that do know about versioning.

The commit message can be stored into the Transaction handle attribute. Whilst this bends a little its intended usage, it also provide a reference message for clients that are unaware of versioning, but that do set some handle message for the sake of error reporting.

New revision information in the response can be stored among the Action elements of a response, since they are designed to carry generic messages too. It would be something like:

.. code-block:: xml 

	<wfs:TransactionResponse ...>
	  <wfs:TransactionSummary>
	    ...
	  </wfs:TransactionSummary>
	  <wfs:TransactionResults>
	    <wfs:Action code="revision" locator="The handle provided in the Transaction request">
	      <wfs:Message>15213</wfs:Message>
	    </wfs:Action>
	  </wfs:TransactionResults>
	  <wfs:InsertResults>
	    ...
	  </wfs:InsertResults>
	</wfs:TransactionResponse>

Rollback and merges can be handled with new elements that leverage the vendor extension mechanisms for Transaction elements. The new RollbackElementType would be very similar to the DeleteElementType, with a typename and a filter, and would require just the specification of the rollbackTo revision as an extra attribute.

.. code-block:: xml 

	<xsd:element name="Rollback" type="wfsv:RollbackType" substitutionGroup="wfs:Native">
	   </xsd:element>
	   <xsd:complexType name="RollbackType">
	      <xsd:complexContent>
	         <xsd:extension base="wfs:NativeType">
	            <xsd:sequence>
	               <xsd:element ref="wfsv:DifferenceQuery" minOccurs="1" maxOccurs="1"/>
	            </xsd:sequence>
	            <xsd:attribute name="handle" type="xsd:string" use="optional"/>
	            <xsd:attribute name="user" type="xsd:string" use="optional"/>
	         </xsd:extension>
	      </xsd:complexContent>
	   </xsd:complexType>

The filter allows to select which features need to be rolled back. This allows for rolling back
changes in a specific area, or with other specific characteristics. The user attribute allows
for gathering only the changes performed by a specific user, so it acts like a filter, but it's separate since the user id is not among the feature type attributes.

Finally, let's handle **conflicts**.
Version control system usually do not allow to commit a change if the server state changed in the meantime, and that's a very basic security measure to avoid losing changes and prevent conflicts.
But in our case, we do want to support versioned WFS unaware clients, so bypassing that mechanism is easy: we have to accept calls that do not specify a reference revision, allowing the overwrite of changes performed between GetFeature and Transaction (unless the client did set a Lock). A configuration parameter should also allow administrators to put unaware clients out of the game, since these unchecked calls are dangerous for data consistency.
Aware clients can leverage extra checks by setting a featureRevision on their update/delete elements, and the server should throw an exception if xxx is not the last revision for the features hit by the update/delete filters. This means the approach is to allow clients to leverage extra check, but without enforcing them.
The extended transaction elements would be:

.. code-block:: xml 

	<xsd:complexType name="VersionedUpdateElementType" >
	      <xsd:complexContent>
	         <xsd:extension base="wfs:UpdateElementType">
	            <xsd:attribute name="featureVersion" type="xsd:string" use="required">
	            </xsd:attribute>
	         </xsd:extension>
	      </xsd:complexContent>
	   </xsd:complexType>
	   <xsd:complexType name="VersionedDeleteElementType" >
	      <xsd:complexContent>
	         <xsd:extension base="wfs:DeleteElementType">
	            <xsd:attribute name="featureVersion" type="xsd:string" use="required">
	            </xsd:attribute>
	         </xsd:extension>
	      </xsd:complexContent>
	   </xsd:complexType>

Reference XSD
-------------

The above snippets have been gathered in an :download:`extension XSD <images/wfsv.xsd>` file that can be analyzed along with OGC ones.
