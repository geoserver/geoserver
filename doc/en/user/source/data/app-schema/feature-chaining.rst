..  _app-schema.feature-chaining:

Feature Chaining
================

Scope
-----

This page describes the use of "Feature Chaining" to compose complex features from simpler components, and in particular to address some requirements that have proven significant in practice.

 * Handling multiple cases of multi-valued properties within a single Feature Type
 * Handing nesting of multi-valued properties within other multi-valued properties
 * Linking related (through association) Feature Types, and in particular allowing re-use of the related features types (for example the O&M pattern has relatedObservation from a samplingFeature, but Observation may be useful in its own right)
 * Encoding the same referenced property object as links when it appears in multiple containing features
 * Eliminating the need for large denormalized data store views of top level features and their related features. Denormalized views would still be needed for special cases, such as many-to-many relationships, but won't be as large.

For non-application schema configurations, please refer to :ref:`app-schema.data-access-integration`.

Mapping steps
-------------

Create a mapping file for every complex type
`````````````````````````````````````````````
We need one mapping file per complex type that is going to be nested, including non features, e.g. gsml:CompositionPart.

Non-feature types that cannot be individually accessed (eg. CompositionPart as a Data Type) can still be mapped separately for its reusability. For this case, the containing feature type has to include these types in its mapping file. The include tag should contain the nested mapping file path relative to the location of the containing type mapping file.
In :download:`GeologicUnit_MappingFile.xml`::

  <includedTypes>	
      <Include>CGITermValue_MappingFile.xml</Include>
      <Include>CompositionPart_MappingFile.xml</Include>
  </includedTypes>

Feature types that can be individually accessed don't need to be explicitly included in the mapping file, as they would be configured for GeoServer to find. 
Such types would have their mapping file associated with a corresponding datastore.xml file, which means that it can be found from the data store registry. 
In other words, if the type is associated with a datastore.xml file, it doesn't need to be explicitly included if referred from another mapping file. 

**Example**:

For this output: :download:`MappedFeature_Output.xml`, here are the mapping files:

   * :download:`MappedFeature_MappingFile.xml`
   * :download:`GeologicUnit_MappingFile.xml`
   * :download:`CompositionPart_MappingFile.xml`
   * :download:`GeologicEvent_MappingFile.xml`
   * :download:`CGITermValue_MappingFile.xml`

*GeologicUnit type*

You can see within GeologicUnit features, both gml:composition (CompositionPart type) and gsml:geologicHistory (GeologicEvent type) are multi-valued properties.
It shows how multiple cases of multi-valued properties can be configured within a single Feature Type. 
This also proves that you can "chain" non-feature type, as CompositionPart is a Data Type.

*GeologicEvent type*

Both gsml:eventEnvironment (CGI_TermValue type) and gsml:eventProcess (also of CGI_TermValue type) are multi-valued properties. 
This also shows that "chaining" can be done on many levels, as GeologicEvent is nested inside GeologicUnit.
Note that gsml:eventAge properties are configured as inline attributes, as there can only be one event age per geologic event, thus eliminating the need for feature chaining. 

Configure nesting on the nested feature type
````````````````````````````````````````````
In the nested feature type, make sure we have a field that can be referenced by the parent feature. 
If there isn't any existing field that can be referred to, the system field *FEATURE_LINK* can be mapped to hold the foreign key value. This is a multi-valued field, so more than one instances can be mapped in the same feature type, for features that can be nested by different parent types. Since this field doesn't exist in the schema, it wouldn't appear in the output document. 

In the source expression tag:

   * OCQL: the value of this should correspond to the OCQL part of the parent feature

**Example One**: Using *FEATURE_LINK* in CGI TermValue type, which is referred by GeologicEvent as gsml:eventProcess and gsml:eventEnvironment. 

In GeologicEvent (the container feature) mapping::

  <AttributeMapping>
	<targetAttribute>gsml:eventEnvironment</targetAttribute>
	<sourceExpression>
		<OCQL>id</OCQL>
		<linkElement>gsml:CGI_TermValue</linkElement>
		<linkField>FEATURE_LINK[1]</linkField>
	</sourceExpression>
	<isMultiple>true</isMultiple>
  </AttributeMapping>
  <AttributeMapping>
	<targetAttribute>gsml:eventProcess</targetAttribute>
	<sourceExpression>
		<OCQL>id</OCQL>
		<linkElement>gsml:CGI_TermValue</linkElement>
		<linkField>FEATURE_LINK[2]</linkField>
	</sourceExpression>
	<isMultiple>true</isMultiple>
  </AttributeMapping>

In CGI_TermValue (the nested feature) mapping::

  <AttributeMapping>
    <!-- FEATURE_LINK[1] is referred by geologic event as environment -->
    <targetAttribute>FEATURE_LINK[1]</targetAttribute>
    <sourceExpression>
        <OCQL>ENVIRONMENT_OWNER</OCQL>
    </sourceExpression>
  </AttributeMapping>
  <AttributeMapping>
    <!-- FEATURE_LINK[2] is referred by geologic event as process -->
    <targetAttribute>FEATURE_LINK[2]</targetAttribute>
    <sourceExpression><
        <OCQL>PROCESS_OWNER</OCQL>
    </sourceExpression>
  </AttributeMapping>

The ENVIRONMENT_OWNER column in CGI_TermValue view corresponds to the ID column in GeologicEvent view.

**Geologic Event property file:**

.. list-table::
   :widths: 15 15 15 15 50

   * - **id**
     - **GEOLOGIC_UNIT_ID:String**
     - **ghminage:String**
     - **ghmaxage:String**
     - **ghage_cdspace:String** 
   * - ge.26931120 
     - gu.25699 
     - Oligocene
     - Paleocene
     - urn:cgi:classifierScheme:ICS:StratChart:2008 
   * - ge.26930473
     - gu.25678
     - Holocene 
     - Pleistocene
     - urn:cgi:classifierScheme:ICS:StratChart:2008 
   * - ge.26930960
     - gu.25678 
     - Pliocene
     - Miocene
     - urn:cgi:classifierScheme:ICS:StratChart:2008 
   * - ge.26932959 
     - gu.25678 
     - LowerOrdovician 
     - LowerOrdovician
     - urn:cgi:classifierScheme:ICS:StratChart:2008  

**CGI Term Value property file:**

.. list-table::
   :widths: 10 30 30 30

   * - **id**
     - **VALUE:String**
     - **PROCESS_OWNER:String**
     - **ENVIRONMENT_OWNER:String** 
   * - 3 
     - fluvial 
     - NULL
     - ge.26931120 
   * - 4  
     - swamp/marsh/bog
     - NULL
     - ge.26930473 
   * - 5 
     - marine 
     - NULL
     - ge.26930960 
   * - 6 
     - submarine fan
     - NULL
     - ge.26932959 
   * - 7 
     - hemipelagic 
     - NULL
     - ge.26932959 
   * - 8 
     - detrital deposition still water 
     - ge.26930473 
     - NULL
   * - 9 
     - water [process] 
     - ge.26932959 
     - NULL
   * - 10 
     - channelled stream flow 
     - ge.26931120 
     - NULL
   * - 11 
     - turbidity current 
     - ge.26932959 
     - NULL

The system field *FEATURE_LINK* doesn't get encoded in the output::

  <gsml:GeologicEvent>                      
    <gml:name codeSpace="urn:cgi:classifierScheme:GSV:GeologicalUnitId">gu.25699</gml:name>
    <gsml:eventAge>
      <gsml:CGI_TermRange>
         <gsml:lower>
            <gsml:CGI_TermValue>   
              <gsml:value codeSpace="urn:cgi:classifierScheme:ICS:StratChart:2008">Oligocene</gsml:value>
            </gsml:CGI_TermValue>
         </gsml:lower>
         <gsml:upper>
            <gsml:CGI_TermValue>
              <gsml:value codeSpace="urn:cgi:classifierScheme:ICS:StratChart:2008">Paleocene</gsml:value>
            </gsml:CGI_TermValue>
         </gsml:upper>
      </gsml:CGI_TermRange>
    </gsml:eventAge>
    <gsml:eventEnvironment>
      <gsml:CGI_TermValue>
         <gsml:value>fluvial</gsml:value>
      </gsml:CGI_TermValue>
    </gsml:eventEnvironment>
    <gsml:eventProcess>
      <gsml:CGI_TermValue>
         <gsml:value>channelled stream flow</gsml:value>
      </gsml:CGI_TermValue>
    </gsml:eventProcess>

**Example Two**:
Using existing field (gml:name) to hold the foreign key, see :download:`MappedFeature_MappingFile.xml`: 

gsml:specification links to gml:name in GeologicUnit::

      <AttributeMapping>
        <targetAttribute>gsml:specification</targetAttribute> 
        <sourceExpression>
          <OCQL>GEOLOGIC_UNIT_ID</OCQL> 
          <linkElement>gsml:GeologicUnit</linkElement> 
          <linkField>gml:name[3]</linkField> 
        </sourceExpression>
      </AttributeMapping>

In :download:`GeologicUnit_MappingFile.xml`: 

GeologicUnit has 3 gml:name properties in the mapping file, so each has a code space to clarify them::  

      <AttributeMapping>
        <targetAttribute>gml:name[1]</targetAttribute> 
        <sourceExpression>
          <OCQL>ABBREVIATION</OCQL> 
        </sourceExpression>
        <ClientProperty>
          <name>codeSpace</name> 
          <value>'urn:cgi:classifierScheme:GSV:GeologicalUnitCode'</value> 
        </ClientProperty>
      </AttributeMapping>
      <AttributeMapping>
        <targetAttribute>gml:name[2]</targetAttribute> 
        <sourceExpression>
          <OCQL>NAME</OCQL> 
        </sourceExpression>
        <ClientProperty>
          <name>codeSpace</name> 
          <value>'urn:cgi:classifierScheme:GSV:GeologicalUnitName'</value> 
        </ClientProperty>
      </AttributeMapping>
      <AttributeMapping>
        <targetAttribute>gml:name[3]</targetAttribute> 
        <sourceExpression>
          <OCQL>id</OCQL> 
        </sourceExpression>
        <ClientProperty>
          <name>codeSpace</name> 
          <value>'urn:cgi:classifierScheme:GSV:MappedFeatureReference'</value> 
        </ClientProperty>
      </AttributeMapping>

The output with multiple gml:name properties and their code spaces::

  <gsml:specification>
    <gsml:GeologicUnit gml:id="gu.25678">
        <gml:description>Olivine basalt, tuff, microgabbro, minor sedimentary rocks</gml:description>
        <gml:name codeSpace="urn:cgi:classifierScheme:GSV:GeologicalUnitCode">-Py</gml:name>
        <gml:name codeSpace="urn:cgi:classifierScheme:GSV:GeologicalUnitName">Yaugher Volcanic Group</gml:name>
        <gml:name codeSpace="urn:cgi:classifierScheme:GSV:MappedFeatureReference">gu.25678</gml:name>

If this is the "one" side of a one-to-many or many-to-one database relationship, we can use the feature id as the source expression field, as you can see in above examples.
See :download:`one_to_many_relationship.JPG` as an illustration.

If we have a many-to-many relationship, we have to use one denormalized view for either side of the nesting. This means we can either use the feature id as the referenced field, or assign a column to serve this purpose. See :download:`many_to_many_relationship.JPG` as an illustration.

.. note:: 

   * For many-to-many relationships, we can't use the same denormalized view for both sides of the nesting.   

Test this configuration by running a getFeature request for the nested feature type on its own.  

Configure nesting on the "containing" feature type
``````````````````````````````````````````````````
When nesting another complex type, you need to specify in your source expression: 

   * **OCQL**: OGC's Common Query Language expression of the data store column
   * **linkElement**: 
       * the nested element name, which is normally the targetElement or mappingName of the corresponding type.
       * on some cases, it has to be an OCQL function (see :ref:`app-schema.polymorphism`)
   * **linkField**: the indexed XPath attribute on the nested element that OCQL corresponds to

**Example:** Nesting composition part in geologic unit feature.

In Geologic Unit mapping file::

  <AttributeMapping>
      <targetAttribute>gsml:composition</targetAttribute>
      <sourceExpression>
	      <OCQL>id</OCQL>
	      <linkElement>gsml:CompositionPart</linkElement>
	      <linkField>FEATURE_LINK</linkField>
      </sourceExpression>
      <isMultiple>true</isMultiple>
  </AttributeMapping>

* *OCQL*: id is the geologic unit id
* *linkElement*: links to gsml:CompositionPart type
* *linkField*: FEATURE_LINK, the linking field mapped in gsml:CompositionPart type that also stores the geologic unit id. If there are more than one of these attributes in the nested feature type, make sure the index is included, e.g. FEATURE_LINK[2]. 

**Geologic Unit property file:**

.. list-table::
   :widths: 15 5 20 60

   * - **id**
     - **ABBREVIATAION:String**
     - **NAME:String**
     - **TEXTDESCRIPTION:String**
   * - gu.25699
     - -Py
     - Yaugher Volcanic Group
     - Olivine basalt, tuff, microgabbro, minor sedimentary rocks
   * - gu.25678
     - -Py 
     - Yaugher Volcanic Group 
     - Olivine basalt, tuff, microgabbro, minor sedimentary rocks  

**Composition Part property file:**

.. list-table::
   :widths: 40 40 20 20

   * - **id**
     - **COMPONENT_ROLE:String**
     - **PROPORTION:String** 
     - **GEOLOGIC_UNIT_ID:String**
   * - cp.167775491936278812 
     - interbedded component 
     - significant  
     - gu.25699 
   * - cp.167775491936278856 
     - interbedded component 
     - minor 
     - gu.25678 
   * - cp.167775491936278844 
     - sole component 
     - major 
     - gu.25678 

Run the getFeature request to test this configuration. Check that the nested features returned in Step 2 are appropriately lined inside the containing features. 
If they are not there, or exceptions are thrown, scroll down and read the "Trouble Shooting" section.

Multiple mappings of the same type
----------------------------------
At times, you may find the need to have different FeatureTypeMapping instances for the same type. 
You may have two different attributes of the same type that need to be nested.
For example, in gsml:GeologicUnit, you have gsml:exposureColor and gsml:outcropCharacter that are both of gsml:CGI_TermValue type.

This is when the optional mappingName tag mentioned in :ref:`app-schema.mapping-file` comes in. 
Instead of passing in the nested feature type's targetElement in the containing type's linkElement, specify the corresponding mappingName. 

.. note::
    * The mappingName is namespace aware and case sensitive.
    * When the referred mappingName contains special characters such as '-', it must be enclosed with single quotes in the linkElement. E.g. <linkElement>'observation-method'</linkElement>.
    * Each mappingName must be unique against other mappingName and targetElement tags across the application. 
    * The mappingName is only to be used to identify the chained type from the nesting type. It is not a solution for multiple FeatureTypeMapping instances where > 1 of them can be queried as top level features. 
    * When queried as a top level feature, the normal targetElement is to be used. Filters involving the nested type should still use the targetElement in the PropertyName part of the query. 
    * You can't have more than 1 FeatureTypeMapping of the same type in the same mapping file if one of them is a top level feature. This is because featuretype.xml would look for the targetElement and wouldn't know which one to get. 
      
The solution for the last point above is to break them up into separate files and locations with only 1 featuretype.xml in the intended top level feature location. 
E.g.

    * You can have 2 FeatureTypeMapping instances in the same file for gsml:CGI_TermValue type since it's not a feature type. 
    * You can have 2 FeatureTypeMapping instances for gsml:MappedFeature, but they have to be broken up into separate files. The one that can be queried as top level feature type would have featuretype.xml in its location. 

Nesting simple properties
-------------------------
You don't need to chain multi-valued simple properties and map them separately. 
The original configuration would still work.

..  _app-schema.filtering-nested:

Filtering nested attributes on chained features
-----------------------------------------------
Filters would work as usual. You can supply the full XPath of the attribute, and the code would handle this.
E.g. You can run the following filter on gsml:MappedFeatureUseCase2A::

  <ogc:Filter>
        <ogc:PropertyIsEqualTo>
            <ogc:Function name="contains_text">
                <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gml:description</ogc:PropertyName>
                <ogc:Literal>Olivine basalt, tuff, microgabbro, minor sedimentary rocks</ogc:Literal>
            </ogc:Function>
            <ogc:Literal>1</ogc:Literal>
        </ogc:PropertyIsEqualTo>
  </ogc:Filter>

..  _app-schema.feature-chaining-by-reference:

Multi-valued properties by reference (*xlink:href*)
---------------------------------------------------
You may want to use feature chaining to set multi-valued properties by reference.
This is particularly handy to avoid endless loop in circular relationships. 
For example, you may have a circular relationship between gsml:MappedFeature and gsml:GeologicUnit.  
E.g.
   
    * gsml:MappedFeature has gsml:GeologicUnit as gsml:specification
    * gsml:GeologicUnit has gsml:MappedFeature as gsml:occurrence

Obviously you can only encode one side of the relationship, or you'll end up with an endless loop.
You would need to pick one side to "chain" and use xlink:href for the other side of the relationship. 

For this example, we are nesting gsml:GeologicUnit in gsml:MappedFeature as gsml:specification.

   * Set up nesting on the container feature type mapping as usual::

      <AttributeMapping>
        <targetAttribute>gsml:specification</targetAttribute>
        <sourceExpression>
            <OCQL>GEOLOGIC_UNIT_ID</OCQL>
	      <linkElement>gsml:GeologicUnit</linkElement>
	      <linkField>gml:name[2]</linkField>
        </sourceExpression>
      </AttributeMapping>

   * Set up xlink:href as client property on the other mapping file::

      <AttributeMapping>
        <targetAttribute>gsml:occurrence</targetAttribute>		
        <sourceExpression>
	      <OCQL>id</OCQL>
	      <linkElement>gsml:MappedFeature</linkElement>
	      <linkField>gsml:specification</linkField>
        </sourceExpression>					              
        <isMultiple>true</isMultiple>			            				
        <ClientProperty>
	       <name>xlink:href</name>
	       <value>strConcat('urn:cgi:feature:MappedFeature:', ID)</value>
        </ClientProperty>     	
      </AttributeMapping>

As we are getting the client property value from a nested feature, we have to set it as if we are chaining the feature; but we also add the client property containing *xlink:href* in the attribute mapping. The code will detect the *xlink:href* setting, and will not proceed to build the nested feature's attributes, and we will end up with empty attributes with *xlink:href* client properties.

This would be the encoded result for gsml:GeologicUnit::

  <gsml:GeologicUnit gml:id="gu.25678">
           <gsml:occurrence xlink:href="urn:cgi:feature:MappedFeature:mf2"/>
           <gsml:occurrence xlink:href="urn:cgi:feature:MappedFeature:mf3"/>

.. note::
   * Don't forget to add *XLink* in your mapping file namespaces section, or you could end up with a StackOverflowException as the *xlink:href* client property won't be recognized and the mappings would chain endlessly.
   * :ref:`app-schema.resolve` may be used to force app-schema to do full feature chaining up to a certain level, even if an xlink reference is specified.
