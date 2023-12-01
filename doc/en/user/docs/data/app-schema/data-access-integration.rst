..  _app-schema.data-access-integration:

Data Access Integration
=======================

This page assumes prior knowledge of :ref:`app-schema` and :ref:`app-schema.feature-chaining`. 
To use feature chaining, the nested features can come from any complex feature data access, as long as:

* it has valid data referred by the "container" feature type,
* the data access is registered via DataAccessRegistry, 
* if FEATURE_LINK is used as the link field, the feature types were created via ComplexFeatureTypeFactoryImpl

However, the "container" features must come from an application schema data access. The rest of this article describes how we can create an application data access from an existing non-application schema data access, in order to "chain" features.
The input data access referred in this article is assumed to be the non-application schema data access. 

How to connect to the input data access
---------------------------------------
Configure the data store connection in "sourceDataStores" tag as usual, but also specify the additional "isDataAccess" tag.
This flag marks that we want to get the registered complex feature source of the specified "sourceType", when processing the source data store.
This assumes that the input data access is registered in DataAccessRegistry upon creation, for the system to find it.

**Example**::

  <sourceDataStores>
    <DataStore>
	<id>EarthResource</id>
	<parameters>
	   <Parameter>
	     <name>directory</name>
	     <value>file:./</value>
	   </Parameter>
	</parameters>
	<isDataAccess>true</isDataAccess>
    </DataStore>
  </sourceDataStores>
  ...
  <typeMappings>
    <FeatureTypeMapping>
      <sourceDataStore>EarthResource</sourceDataStore>
	<sourceType>EarthResource</sourceType>
  ...

How to configure the mapping
----------------------------
Use "inputAttribute" in place of "OCQL" tag inside "sourceExpression", to specify the input XPath expressions.

**Example**::

  <AttributeMapping>
    <targetAttribute>gsml:classifier/gsml:ControlledConcept/gsml:preferredName</targetAttribute>
    <sourceExpression>
        <inputAttribute>mo:classification/mo:MineralDepositModel/mo:mineralDepositGroup</inputAttribute>
    </sourceExpression>
  </AttributeMapping>

How to chain features
---------------------
Feature chaining works both ways for the re-mapped complex features. You can chain other features inside these features, and vice-versa. 
The only difference is to use "inputAttribute" for the input XPath expressions, instead of "OCQL" as mentioned above. 
 
**Example**:: 

  <AttributeMapping>
    <targetAttribute>gsml:occurence</targetAttribute>
    <sourceExpression>
        <inputAttribute>mo:commodityDescription</inputAttribute>
        <linkElement>gsml:MappedFeature</linkElement>
        <linkField>gml:name[2]</linkField>
    </sourceExpression>
    <isMultiple>true</isMultiple>
  </AttributeMapping>
 
How to use filters
------------------
From the user point of view, filters are configured as per normal, using the mapped/output target attribute XPath expressions. 
However, when one or more attributes in the expression is a multi-valued property, we need to specify a function such as "contains_text" in the filter. 
This is because when multiple values are returned, comparing them to a single value would only return true if there is only one value returned, and it is the same value. 
Please note that the "contains_text" function used in the following example is not available in GeoServer API, but defined in the database. 

**Example:**

Composition is a multi-valued property::

  <ogc:Filter>
    <ogc:PropertyIsEqualTo>
      <ogc:Function name="contains_text">
          <ogc:PropertyName>gsml:composition/gsml:CompositionPart/gsml:proportion/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
          <ogc:Literal>Olivine basalt, tuff, microgabbro, minor sedimentary rocks</ogc:Literal>
      </ogc:Function>
      <ogc:Literal>1</ogc:Literal>
    </ogc:PropertyIsEqualTo>
  </ogc:Filter>
