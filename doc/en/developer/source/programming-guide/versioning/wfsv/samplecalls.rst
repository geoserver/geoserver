.. _versioning_implementation_samplecalls:

Versioning WFS - Sample calls
============================== 

The sample versioning configuration contains a set of sample calls that can be performed against the versioning datastore to try out its functionality.
Here are the calls with some narrative explaining them.

Assess initial situation
------------------------

All of the calls hit the topp:archsites feature type. This call extract two of the three feature we're going to modify with the transaction.

.. code-block:: xml

	<wfs:GetFeature service="WFSV" version="1.0.0"
	  xmlns:topp="http://www.openplans.org/topp"
	  xmlns:wfs="http://www.opengis.net/wfs"
	  xmlns:ogc="http://www.opengis.net/ogc"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://www.opengis.net/wfs
	                      http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd">
	  <wfs:Query typeName="topp:archsites">
	    <ogc:Filter>
	       <ogc:FeatureId fid="archsites.1"/>
	       <ogc:FeatureId fid="archsites.2"/>
	       <ogc:FeatureId fid="archsites.25"/>
	    </ogc:Filter>
	  </wfs:Query>
	</wfs:GetFeature>

This is a standard WFS GetFeature, but you may have noticed that the service hit is "WFSV".
Also, if you run the request you'll notice archsites.25 is not there, because we're going to create it with the next request.

Transaction
-----------

This is a standard WFS transaction, versioning is totally transparent (that is, this transaction call is exactly the same you would perform on a standard WFS, the difference is that the backend will record the change instead of just applying it to the data).

.. code-block:: xml

	<wfs:Transaction service="WFSV" version="1.0.0"
	  xmlns:topp="http://www.openplans.org/topp"
	  xmlns:ogc="http://www.opengis.net/ogc" xmlns:wfs="http://www.opengis.net/wfs"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xmlns:gml="http://www.opengis.net/gml"
	  xsi:schemaLocation="http://www.opengis.net/wfs
	                      http://schemas.opengis.net/wfs/1.0.0/WFS-transaction.xsd
	                      http://www.openplans.org/topp
	                      http://localhost:8080/geoserver/wfsv?request=DescribeFeatureType&amp;version=1.0.0&amp;typeName=topp:archsites"
	  handle="Updating Signature rock label">
	  <wfs:Insert>
	    <topp:archsites>
	      <topp:cat>2</topp:cat>
	      <topp:str1>Alien crash site</topp:str1>
	      <topp:the_geom>
	        <gml:Point srsName="http://www.opengis.net/gml/srs/epsg.xml#26713">
	          <gml:coordinates decimal="." cs="," ts=" ">604000,4930000</gml:coordinates>
	        </gml:Point>
	      </topp:the_geom>
	    </topp:archsites>
	  </wfs:Insert>
	  <wfs:Update typeName="topp:archsites">
	    <wfs:Property>
	      <wfs:Name>str1</wfs:Name>
	      <wfs:Value>Signature Rock, updated</wfs:Value>
	    </wfs:Property>
	    <ogc:Filter>
	      <ogc:FeatureId fid="archsites.1" />
	    </ogc:Filter>
	  </wfs:Update>
	  <wfs:Delete typeName="topp:archsites">
	    <ogc:Filter>
	      <ogc:FeatureId fid="archsites.2" />
	    </ogc:Filter>
	  </wfs:Delete>
	</wfs:Transaction>

The result is:

.. code-block:: xml

	<?xml version="1.0" encoding="UTF-8"?>
	<wfs:WFS_TransactionResponse version="1.0.0" xmlns:wfs="http://www.opengis.net/wfs"
	  xmlns:ogc="http://www.opengis.net/ogc" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://www.opengis.net/wfs http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-transaction.xsd">
	  <wfs:InsertResult>
	    <ogc:FeatureId fid="archsites.26" />
	  </wfs:InsertResult>
	  <wfs:TransactionResult handle="Updating Signature rock label">
	    <wfs:Status>
	      <wfs:SUCCESS />
	    </wfs:Status>
	  </wfs:TransactionResult>
	</wfs:WFS_TransactionResponse>

Grabbing the versioning log
---------------------------

The following call retrieves the change log for the topp:archsites feature type:

.. code-block:: xml

	<wfsv:GetLog service="WFSV" version="1.0.0"
	  outputFormat="HTML"
	  xmlns:topp="http://www.openplans.org/topp"
	  xmlns:ogc="http://www.opengis.net/ogc"
	  xmlns:wfs="http://www.opengis.net/wfs"
	  xmlns:wfsv="http://www.opengis.net/wfsv"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://www.opengis.net/wfsv
	                      http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-versioning.xsd">
	  <wfsv:DifferenceQuery typeName="topp:archsites" fromFeatureVersion="1" toFeatureVersion="13"/>
	</wfsv:GetLog>

resulting in a single entry (the entries from 1 to 6 report the operations needed to version enable the tables). Note how the transaction handle was used as the commit message, and also the fact the user is unknown, because in this demo the http basic authentication was not used.

+----------+--------------+--------------+-------------------------------+
| Revision | Author       | Date         | Message                       |
+==========+==============+==============+===============================+
|7         | anonymous    |10/10/07 9.47 | Updating Signature rock label |
+----------+--------------+--------------+-------------------------------+

Alternatively, the GetLog result can be encoded in GML.
GetFeature with version support

Let's do a before and after comparison. This first GetFeature tries to retrieve the value of the features at revision 1.

.. code-block:: xml

	<wfs:GetFeature service="WFSV" version="1.0.0"
	  outputFormat="GML2"
	  xmlns:topp="http://www.openplans.org/topp"
	  xmlns:wfs="http://www.opengis.net/wfs"
	  xmlns:ogc="http://www.opengis.net/ogc"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://www.opengis.net/wfs
	                      http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd">
	  <wfs:Query typeName="topp:archsites" featureVersion="1">
	    <ogc:Filter>
	       <ogc:FeatureId fid="archsites.1"/>
	       <ogc:FeatureId fid="archsites.2"/>
	       <ogc:FeatureId fid="archsites.26"/>
	    </ogc:Filter>
	  </wfs:Query>
	</wfs:GetFeature>

This one retrieves the results at revision 1, that is, before the transaction occurred. We can see features archsites.1 and archsites.2, whilst archsites.26 is not there (it has been created at revision 7).

.. code-block:: xml

	<?xml version="1.0" encoding="UTF-8"?>
	<wfs:FeatureCollection xmlns="http://www.opengis.net/wfs" xmlns:wfs="http://www.opengis.net/wfs"
	  xmlns:topp="http://www.openplans.org/topp" xmlns:gml="http://www.opengis.net/gml"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://www.openplans.org/topp http://localhost:8080/geoserver/wfs?service=WFS&amp;version=1.0.0&amp;request=DescribeFeatureType&amp;typeName=topp:archsites http://www.opengis.net/wfs http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-basic.xsd">
	  <gml:boundedBy>
	    <gml:Box srsName="http://www.opengis.net/gml/srs/epsg.xml#26713">
	      <gml:coordinates xmlns:gml="http://www.opengis.net/gml" decimal="." cs="," ts=" ">
	        591950,4914730 593493,4923000
	      </gml:coordinates>
	    </gml:Box>
	  </gml:boundedBy>
	  <gml:featureMember>
	    <topp:archsites fid="archsites.1">
	      <topp:cat>1</topp:cat>
	      <topp:str1>Signature Rock</topp:str1>
	      <topp:the_geom>
	        <gml:Point srsName="http://www.opengis.net/gml/srs/epsg.xml#26713">
	          <gml:coordinates xmlns:gml="http://www.opengis.net/gml" decimal="." cs="," ts=" ">
	            593493,4914730
	          </gml:coordinates>
	        </gml:Point>
	      </topp:the_geom>
	    </topp:archsites>
	  </gml:featureMember>
	  <gml:featureMember>
	    <topp:archsites fid="archsites.2">
	      <topp:cat>2</topp:cat>
	      <topp:str1>No Name</topp:str1>
	      <topp:the_geom>
	        <gml:Point srsName="http://www.opengis.net/gml/srs/epsg.xml#26713">
	          <gml:coordinates xmlns:gml="http://www.opengis.net/gml" decimal="." cs="," ts=" ">
	            591950,4923000
	          </gml:coordinates>
	        </gml:Point>
	      </topp:the_geom>
	    </topp:archsites>
	  </gml:featureMember>
	</wfs:FeatureCollection>

Querying for revision 7 offers a different result:

.. code-block:: xml

	<wfs:GetFeature service="WFSV" version="1.0.0"
	  outputFormat="GML2"
	  xmlns:topp="http://www.openplans.org/topp"
	  xmlns:wfs="http://www.opengis.net/wfs"
	  xmlns:ogc="http://www.opengis.net/ogc"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://www.opengis.net/wfs
	                      http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd">
	  <wfs:Query typeName="topp:archsites">
	    <ogc:Filter>
	       <ogc:FeatureId fid="archsites.1"/>
	       <ogc:FeatureId fid="archsites.2"/>
	       <ogc:FeatureId fid="archsites.26"/>
	    </ogc:Filter>
	  </wfs:Query>
	</wfs:GetFeature>

.. code-block:: xml

	<?xml version="1.0" encoding="UTF-8"?>
	<wfs:FeatureCollection xmlns="http://www.opengis.net/wfs" xmlns:wfs="http://www.opengis.net/wfs"
	  xmlns:topp="http://www.openplans.org/topp" xmlns:gml="http://www.opengis.net/gml"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://www.openplans.org/topp http://localhost:8080/geoserver/wfs?service=WFS&amp;version=1.0.0&amp;request=DescribeFeatureType&amp;typeName=topp:archsites http://www.opengis.net/wfs http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-basic.xsd">
	  <gml:boundedBy>
	    <gml:Box srsName="http://www.opengis.net/gml/srs/epsg.xml#26713">
	      <gml:coordinates xmlns:gml="http://www.opengis.net/gml" decimal="." cs="," ts=" ">
	        593493,4914730 604000,4930000
	      </gml:coordinates>
	    </gml:Box>
	  </gml:boundedBy>
	  <gml:featureMember>
	    <topp:archsites fid="archsites.26">
	      <topp:cat>2</topp:cat>
	      <topp:str1>Alien crash site</topp:str1>
	      <topp:the_geom>
	        <gml:Point srsName="http://www.opengis.net/gml/srs/epsg.xml#26713">
	          <gml:coordinates xmlns:gml="http://www.opengis.net/gml" decimal="." cs="," ts=" ">
	            604000,4930000
	          </gml:coordinates>
	        </gml:Point>
	      </topp:the_geom>
	    </topp:archsites>
	  </gml:featureMember>
	  <gml:featureMember>
	    <topp:archsites fid="archsites.1">
	      <topp:cat>1</topp:cat>
	      <topp:str1>Signature Rock, updated</topp:str1>
	      <topp:the_geom>
	        <gml:Point srsName="http://www.opengis.net/gml/srs/epsg.xml#26713">
	          <gml:coordinates xmlns:gml="http://www.opengis.net/gml" decimal="." cs="," ts=" ">
	            593493,4914730
	          </gml:coordinates>
	        </gml:Point>
	      </topp:the_geom>
	    </topp:archsites>
	  </gml:featureMember>
	</wfs:FeatureCollection>

Here we can see archsites.2 has been removed (deleted during the transaction) and archsites.26 appears.

GetDiff
-------

Diff returns the difference between two revisions, eventually it's possible to specify a filter to gather the diff concerning a specific feature set.
Also notice that the output format is HTML, but if you don't specify it, you'll get the log encoded as a WFS Transaction (the transaction that applied to the initial revision bring you to the specified destination revision).

.. code-block:: xml

	<wfsv:GetDiff service="WFSV" version="1.0.0"
	  outputFormat="HTML"
	  xmlns:topp="http://www.openplans.org/topp"
	  xmlns:ogc="http://www.opengis.net/ogc"
	  xmlns:wfs="http://www.opengis.net/wfs"
	  xmlns:wfsv="http://www.opengis.net/wfsv"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://www.opengis.net/wfsv
	  http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-versioning.xsd">
	  <wfsv:DifferenceQuery typeName="topp:archsites" fromFeatureVersion="1"/>
	</wfsv:GetDiff>

The output, in human readable HTML format, is::

	Feature type 'archsites', diff from version 1 to version CURRENT
	
	Feature archsites.26, inserted, feature content:
	
	    * cat: 2
	    * str1: Alien crash site
	    * the_geom: POINT (604000 4930000)
	
	Feature archsites.2, deleted, old feature content:
	
	    * cat: 2
	    * str1: No Name
	    * the_geom: POINT (591950 4923000)
	
	Feature archsites.1, updated, modified attributes:
	+-----------+------------------+------------------------+
	| Attribute | Value at 1       | Value at CURRENT       |
	+===========+==================+========================+
	| str1      | Signature Rock   |Signature Rock, updated |
	+-----------+------------------+------------------------+

If no output format is specified, the GetDiff will return a WFS transaction, that is, the actions needed to turn the features from version 1 to version CURRENT.
The GetDiff can also work backwards, that is, one could specify toFeatureVersion < fromFeatureVersion, this would return a reverse diff, which seen as a transaction, is the set of command needed to rollback the changes. Yet, it's advisable to use the specific Rollback element for actually performing rollbacks, because it guarantees deleted features are restored with the original feature id (perfoming the transaction returned by a backwards diff would create a new feature id instead).

Rollback
--------

Versioning WFS introduces a new transaction element, Rollback, that can be used to undo changes performed between two revisions, eventually limiting the rollback to features touched by a specific user, or satisfying a specific filter.

.. code-block:: xml

	<wfs:Transaction service="WFSV" version="1.0.0"
	  xmlns:topp="http://www.openplans.org/topp"
	  xmlns:ogc="http://www.opengis.net/ogc"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xmlns:gml="http://www.opengis.net/gml"
	  xmlns:wfsv="http://www.opengis.net/wfsv"
	  xmlns:wfs="http://www.opengis.net/wfs"
	  xsi:schemaLocation="http://www.opengis.net/wfsv
	                      http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-versioning.xsd">
	  handle="Rolling back previous changes">
	  <wfsv:Rollback safeToIgnore="false" vendorId="TOPP" typeName="archsites" toFeatureVersion="1"/>
	</wfs:Transaction>

and the result would be:

.. code-block:: xml

	<?xml version="1.0" encoding="UTF-8"?>
	<wfs:WFS_TransactionResponse version="1.0.0" xmlns:wfs="http://www.opengis.net/wfs"
	  xmlns:ogc="http://www.opengis.net/ogc" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xsi:schemaLocation="http://www.opengis.net/wfs http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-transaction.xsd">
	  <wfs:InsertResult>
	    <ogc:FeatureId fid="none" />
	  </wfs:InsertResult>
	  <wfs:TransactionResult>
	    <wfs:Status>
	      <wfs:SUCCESS />
	    </wfs:Status>
	  </wfs:TransactionResult>
	</wfs:WFS_TransactionResponse>

Versioning aware clients and transactions
-----------------------------------------

Versioning aware clients should try to handle updates keeping the possibily of conflicts in mind. A conflict occurrs if the same feature
is modified by two users in the same time frame. Suppose the first one managed to commit its changes.
The second one should try to avoid overwriting the first user changes, and instead be notified that it does not have the latest revision.
In that case, the client should first update (by getting a diff), handle the eventual conflict locally, and then try to commit again.

The following example shows a transaction that should fail, because the client declares of being at a revision number
that's no more the latest revision:

.. code-block:: xml

	<wfs:Transaction service="WFSV" version="1.0.0"
	  xmlns:topp="http://www.openplans.org/topp"
	  xmlns:ogc="http://www.opengis.net/ogc"
	  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	  xmlns:gml="http://www.opengis.net/gml"
	  xmlns:wfsv="http://www.opengis.net/wfsv"
	  xmlns:wfs="http://www.opengis.net/wfs"
	  xsi:schemaLocation="http://www.opengis.net/wfsv
	                      http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-versioning.xsd">
	  handle="Trying an update with wrong version">
	  <wfsv:VersionedUpdate typeName="topp:archsites" featureVersion="1">
	    <wfs:Property>
	      <wfs:Name>str1</wfs:Name>
	      <wfs:Value>You won't see me updated</wfs:Value>
	    </wfs:Property>
	    <ogc:Filter>
	      <ogc:FeatureId fid="archsites.1" />
	    </ogc:Filter>
	  </wfsv:VersionedUpdate>
	</wfs:Transaction>

