..  _app-schema.polymorphism:

Polymorphism
============

Polymorphism in this context refers to the ability of an attribute to have different forms.
Depending on the source value, it could be encoded with a specific structure, type, as an xlink:href reference, or not encoded at all.
To achieve this, we reuse feature chaining syntax and allow OCQL functions in the linkElement tag.
Read more about :ref:`app-schema.feature-chaining`, if you're not familiar with the syntax.


Data-type polymorphism
----------------------
You can use normal feature chaining to get an attribute to be encoded as a certain type. 
For example::

  <AttributeMapping>                                                                                                                                                                                                                                                                                                                                                                                                    
      <targetAttribute>ex:someAttribute</targetAttribute>
      <sourceExpression>
          <OCQL>VALUE_ID</OCQL>
	    <linkElement>NumericType</linkElement>
	    <linkField>FEATURE_LINK</linkField>
      </sourceExpression>
  </AttributeMapping>
  <AttributeMapping>
      <targetAttribute>ex:someAttribute</targetAttribute>
      <sourceExpression>
	    <OCQL>VALUE_ID</OCQL>
	    <linkElement>gsml:CGI_TermValue</linkElement>
	    <linkField>FEATURE_LINK</linkField>
      </sourceExpression>
  </AttributeMapping>

Note: NumericType here is a mappingName, whereas gsml:CGI_TermValue is a targetElement.

In the above example, ex:someAttribute would be encoded with the configuration in NumericType if the foreign key matches the linkField.
Both instances would be encoded if the foreign key matches the candidate keys in both linked configurations. 
Therefore this would only work for 0 to many relationships.

Functions can be used for single attribute instances. See `useful functions`_ for a list of commonly used functions. Specify the function in the linkElement, and it would map it to the first matching FeatureTypeMapping.
For example::

  <AttributeMapping>
      <targetAttribute>ex:someAttribute</targetAttribute>	
      <sourceExpression>
	    <OCQL>VALUE_ID</OCQL>	
	    <linkElement>
	      Recode(CLASS_TEXT, 'numeric', 'NumericType', 'literal', 'gsml:CGI_TermValue')
          </linkElement>
  	    <linkField>FEATURE_LINK</linkField>
      </sourceExpression>					
      <isMultiple>true</isMultiple>
  </AttributeMapping>

The above example means, if the CLASS_TEXT value is 'numeric', it would link to 'NumericType' FeatureTypeMapping, with VALUE_ID as foreign key to the linked type.
It would require all the potential matching types to have a common attribute that is specified in linkField. In this example, the linkField is FEATURE_LINK, which is a fake attribute used only for feature chaining.
You can omit the linkField and OCQL if the FeatureTypeMapping being linked to has the same sourceType with the container type. 
This would save us from unnecessary extra queries, which would affect performance. 
For example:

FeatureTypeMapping of the container type::

  <FeatureTypeMapping>
      <sourceDataStore>PropertyFiles</sourceDataStore>
      <sourceType>PolymorphicFeature</sourceType>

FeatureTypeMapping of NumericType points to the same table::

  <FeatureTypeMapping>
      <mappingName>NumericType</mappingName>
      <sourceDataStore>PropertyFiles</sourceDataStore>
      <sourceType>PolymorphicFeature</sourceType>

FeatureTypeMapping of gsml:CGI_TermValue also points to the same table::

  <FeatureTypeMapping>
      <sourceDataStore>PropertyFiles</sourceDataStore>
      <sourceType>PolymorphicFeature</sourceType>
      <targetElement>gsml:CGI_TermValue</targetElement>		

In this case, we can omit linkField in the polymorphic attribute mapping::

  <AttributeMapping>
      <targetAttribute>ex:someAttribute</targetAttribute>	
      <sourceExpression>
 	    <linkElement>
	      Recode(CLASS_TEXT, 'numeric', 'NumericType', 'literal', 'gsml:CGI_TermValue')
          </linkElement>
      </sourceExpression>					
      <isMultiple>true</isMultiple>
  </AttributeMapping>


Referential polymorphism
------------------------
This is when an attribute is set to be encoded as an xlink:href reference on the top level.
When the scenario only has reference cases in it, setting a function in Client Property will do the job. E.g.::

    <AttributeMapping>
	  <targetAttribute>ex:someAttribute</targetAttribute>
	  <ClientProperty>
		<name>xlink:href</name>
		<value>if_then_else(isNull(NUMERIC_VALUE), 'urn:ogc:def:nil:OGC:1.0:missing', strConcat('#', NUMERIC_VALUE))</value>
        </ClientProperty>
    </AttributeMapping>

The above example means, if NUMERIC_VALUE is null, the attribute should be encoded as::
   
   <ex:someAttribute xlink:href="urn:ogc:def:nil:OGC:1.0:missing">

Otherwise, it would be encoded as::

   <ex:someAttribute xlink:href="#123">
       where NUMERIC_VALUE = '123'

However, this is not possible when we have cases where a fully structured attribute is also a possibility.
The `toxlinkhref`_ function can be used for this scenario. E.g.::

    <AttributeMapping>
        <targetAttribute>ex:someAttribute</targetAttribute>	
        <sourceExpression>
 	      <linkElement>
	        if_then_else(isNull(NUMERIC_VALUE), toXlinkHref('urn:ogc:def:nil:OGC:1.0:missing'), 
                  if_then_else(lessEqualThan(NUMERIC_VALUE, 1000), 'numeric_value', toXlinkHref('urn:ogc:def:nil:OGC:1.0:missing'))) 
            </linkElement>
        </sourceExpression>	
    </AttributeMapping>

The above example means, if NUMERIC_VALUE is null, the output would be encoded as::

    <ex:someAttribute xlink:href="urn:ogc:def:nil:OGC:1.0:missing">

Otherwise, if NUMERIC_VALUE is less or equal than 1000, it would be encoded with attributes from FeatureTypeMapping with 'numeric_value' mappingName.
If NUMERIC_VALUE is greater than 1000, it would be encoded as the first scenario.


Useful functions
----------------
if_then_else function
`````````````````````

**Syntax**:: 

  if_then_else(BOOLEAN_EXPRESSION, value, default value) 

* **BOOLEAN_EXPRESSION**: could be a Boolean column value, or a Boolean function 
* **value**: the value to map to, if BOOLEAN_EXPRESSION is true
* **default value**: the value to map to, if BOOLEAN_EXPRESSION is false

Recode function
```````````````

**Syntax**::

  Recode(EXPRESSION, key1, value1, key2, value2,...)

* **EXPRESSION**: column name to get values from, or another function
* **key-n**: 
    * key expression to map to value-n
    * if the evaluated value of EXPRESSION doesn't match any key, nothing would be encoded for the attribute.
* **value-n**: value expression which translates to a mappingName or targetElement

lessEqualThan
`````````````
Returns true if ATTRIBUTE_EXPRESSION evaluates to less or equal than LIMIT_EXPRESSION.

**Syntax**::
  
  lessEqualThan(ATTRIBUTE_EXPRESSION, LIMIT_EXPRESSION)

* **ATTRIBUTE_EXPRESSION**: expression of the attribute being evaluated.
* **LIMIT_EXPRESSION**: expression of the numeric value to be compared against.

lessThan
````````
Returns true if ATTRIBUTE_EXPRESSION evaluates to less than LIMIT_EXPRESSION.

**Syntax**::
  
  lessThan(ATTRIBUTE_EXPRESSION, LIMIT_EXPRESSION)

* **ATTRIBUTE_EXPRESSION**: expression of the attribute being evaluated.
* **LIMIT_EXPRESSION**: expression of the numeric value to be compared against.

equalTo
```````
Compares two expressions and returns true if they're equal. 

**Syntax**::
  
  equalTo(LHS_EXPRESSION, RHS_EXPRESSION)

isNull
``````
Returns a Boolean that is true if the expression evaluates to null.

**Syntax**::

  isNull(EXPRESSION)

* **EXPRESSION**: expression to be evaluated.

toXlinkHref
```````````
Special function written for referential polymorphism and feature chaining, not to be used outside of linkElement.
It infers that the attribute should be encoded as xlink:href. 

**Syntax**::
  
  toXlinkHref(XLINK_HREF_EXPRESSION)

* **XLINK_HREF_EXPRESSION**: 
    * could be a function or a literal
    * has to be wrapped in single quotes if it's a literal

.. note:: 
    * To get toXlinkHref function working, you need to declare xlink URI in the namespaces. 

Other functions
```````````````
Please refer to :ref:`filter_function_reference`. 

Combinations
````````````
You can combine functions, but it might affect performance.
E.g.::

    if_then_else(isNull(NUMERIC_VALUE), toXlinkHref('urn:ogc:def:nil:OGC:1.0:missing'), 
        if_then_else(lessEqualThan(NUMERIC_VALUE, 1000), 'numeric_value', toXlinkHref('urn:ogc:def:nil:OGC:1.0:missing'))) 
           

.. note:: 
    * When specifying a mappingName or targetElement as a value in functions, make sure they're enclosed in single quotes. 
    * Some functions have no null checking, and will fail when they encounter null. 
    * The workaround for this is to wrap the expression with isNull() function if null is known to exist in the data set.


Null or missing value 
---------------------
To skip the attribute for a specific case, you can use Expression.NIL as a value in if_then_else or not include the key in `Recode function`_ .
E.g.::
    
    if_then_else(isNull(VALUE), Expression.NIL, 'gsml:CGI_TermValue')
        means the attribute would not be encoded if VALUE is null.

    Recode(VALUE, 'term_value', 'gsml:CGI_TermValue')
        means the attribute would not be encoded if VALUE is anything but 'term_value'. 

To encode an attribute as xlink:href that represents missing value on the top level, see `Referential Polymorphism`_.


Any type 
--------
Having xs:anyType as the attribute type itself infers that it is polymorphic, since they can be encoded as any type.

If the type is pre-determined and would always be the same, we might need to specify :ref:`app-schema.mapping-file.targetAttributeNode`.
E.g.::

    <AttributeMapping>
          <targetAttribute>om:result</targetAttribute>
          <targetAttributeNode>gml:MeasureType<targetAttributeNode>
          <sourceExpression>
              <OCQL>TOPAGE</OCQL>
          </sourceExpression>
          <ClientProperty>
              <name>xsi:type</name>
              <value>'gml:MeasureType'</value>
          </ClientProperty>
          <ClientProperty>
              <name>uom</name> 
              <value>'http://www.opengis.net/def/uom/UCUM/0/Ma'</value>
          </ClientProperty> 
    </AttributeMapping>

If the casting type is complex, this is not a requirement as app-schema is able to automatically determine the type from the XPath in targetAttribute.
E.g., in this example ``om:result`` is automatically specialised as a MappedFeatureType::

    <AttributeMapping>
          <targetAttribute>om:result/gsml:MappedFeature/gml:name</targetAttribute>
          <sourceExpression>
              <OCQL>NAME</OCQL>
          </sourceExpression>
    </AttributeMapping>

Alternatively, we can use feature chaining. For the same example above, the mapping would be::

    <AttributeMapping>
	  <targetAttribute>om:result</targetAttribute>
	  <sourceExpression>
		<OCQL>LEX_D</OCQL>
		<linkElement>gsml:MappedFeature</linkElement>
		<linkField>gml:name</linkField>
	  </sourceExpression>
    </AttributeMapping>	

If the type is conditional, the mapping style for such attributes is the same as any other polymorphic attributes. E.g.::

    <AttributeMapping>
	  <targetAttribute>om:result</targetAttribute>
	  <sourceExpression>
		<linkElement>
		   Recode(NAME, Expression.Nil, toXlinkHref('urn:ogc:def:nil:OGC::missing'),'numeric',
                   toXlinkHref(strConcat('urn:numeric-value::', NUMERIC_VALUE)), 'literal', 'TermValue2')
		</linkElement>
	  </sourceExpression>
    </AttributeMapping>


Filters
-------
Filters should work as usual, as long as the users know what they want to filter. 
For example, when an attribute could be encoded as gsml:CGI_TermValue or gsml:CGI_NumericValue, users can run filters with property names of:

    * ex:someAttribute/gsml:CGI_TermValue/gsml:value to return matching attributes that are encoded as gsml:CGI_TermValue and satisfy the filter.
    * likewise, ex:someAttribute/gsml:CGI_NumericValue/gsml:principalValue should return matching gsml:CGI_NumericValue attributes.

Another limitation is filtering attributes of an xlink:href attribute pointing to an instance outside of the document.  
