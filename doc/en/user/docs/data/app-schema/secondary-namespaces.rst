.. _app-schema.secondary-namespaces:

Secondary Namespaces
====================

What is a secondary namespace?
``````````````````````````````
A secondary namespace is one that is referenced indirectly by the main schema, that is, one schema imports another one as shown below::

    a.xsd imports b.xsd
    b.xsd imports c.xsd

(using a, b and c as the respective namespace prefixes for a.xsd, b.xsd and c.xsd)::

    a.xsd declares b:prefix
    b.xsd declares c:prefix

The GeoTools encoder does not honour these namespaces and writes out::

"a:" , "b:" but NOT "c:"

The result is c's element being encoded as::

 <null:cElement/>


When to configure for secondary namespaces
``````````````````````````````````````````
If your application spans several namespaces which may be very common in application schemas.

A sure sign that calls for secondary namespace configuration is when prefixes for namespaces are printed out as the literal string "null" or error messages like::

    java.io.IOException: The prefix "null" for element "null:something" is not bound.
    
.. note::

   When using secondary namespaces, requests involving complex featuretypes
   must be made to the **global OWS service** only, not to :ref:`virtual_services`.  
   This is because virtual services are restricted to a single namespace, 
   and thus are not able to access secondary namespaces.


In order to allow GeoServer App-Schema to support secondary namespaces, please follow the steps outlined below:

Using the sampling namespace as an example.

Step 1:Create the Secondary Namespace folder
````````````````````````````````````````````
Create a folder to represent the secondary namespace in the data/workspaces directory, 
in our example that will be the "sa" folder.

Step 2:Create files
````````````````````
Create  two files below in the "sa" folder: 

#. namespace.xml
#. workspace.xml

Step 3:Edit content of files
````````````````````````````

Contents of these files are as follows:

namespace.xml(uri is a valid uri for the secondary namespace, in this case the sampling namespace uri)::

	<namespace>
		<id>sa_workspace</id>	
		<prefix>sa</prefix>
		<uri>http://www.opengis.net/sampling/1.0</uri>
	</namespace> 
	
workspace.xml::

	<workspace>
		<id>sa_workspace</id>	
		<name>sa</name>
	</workspace> 

That's it. 

Your workspace is now configured to use a Secondary Namespace.
