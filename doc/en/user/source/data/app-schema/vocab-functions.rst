..  _app-schema.vocab-functions:

Vocabulary functions
====================

Scope
-----

This page describes how to serve vocabulary translations using some function expressions in application schema mapping file.
If you're not familiar with application schema mapping file, read :ref:`app-schema.mapping-file`.

Versions supported
``````````````````
This functionality is supported from GeoTools version 2.6-M2 onwards.

Useful functions
----------------

Recode function
```````````````````
This is similar to *if_then_else* function, except that there is no default clause. 
You have to specify a translation value for every vocabulary key.

**Syntax**::

  Recode(COLUMN_NAME, key1, value1, key2, value2,...)

* **COLUMN_NAME**: column name to get values from

**Example**::

  <AttributeMapping>
    <targetAttribute>gml:name</targetAttribute>
    <sourceExpression>
        <OCQL>Recode(ABBREVIATION, '1GRAV', 'urn:cgi:classifier:CGI:SimpleLithology:2008:gravel',
                                   '1TILL', 'urn:cgi:classifier:CGI:SimpleLithology:2008:diamictite',
                                   '6ALLU', 'urn:cgi:classifier:CGI:SimpleLithology:2008:sediment')
        </OCQL>
    </sourceExpression>
  </AttributeMapping>

The above example will map **gml:name** value to *urn:cgi:classifier:CGI:SimpleLithology:2008:gravel* if the ABBREVIATION column value is *1GRAV*.

Categorize function
```````````````````
This is more suitable for numeric keys, where the translation value is determined by the key's position within the thresholds.

**Syntax**::

  Categorize(COLUMN_NAME, default_value, threshold 1, value 1, threshold 2, value 2, ..., [preceding/succeeding])

* **COLUMN_NAME**: data source column name
* **default_value**: default value to be mapped if COLUMN_NAME value is not within the threshold
* **threshold(n)**: threshold value
* **value(n)**: value to be mapped if the threshold is met
* **preceding/succeeding**:
    - optional, succeeding is used by default if not specified.
    - not case sensitive.
    - preceding: value is within threshold if COLUMN_NAME value > threshold
    - succeeding: value is within threshold if COLUMN_NAME value >= threshold

**Example**::

  <AttributeMapping>
    <targetAttribute>gml:description</targetAttribute>
    <sourceExpression>
        <OCQL>Categorize(CGI_LOWER_RANGE, 'missing_value', 1000, 'minor', 5000, 'significant')</OCQL>
    </sourceExpression>
  </AttributeMapping>

The above example means **gml:description** value would be *significant* if CGI_LOWER_RANGE column value is >= *5000*.


Vocab function
``````````````
This is the new function Jody implemented, and more useful for bigger vocabulary pairs.
Instead of writing a long key-to-value pairs in the function, you can keep them in a separate properties file.
The properties file serves as a lookup table to the function. It has no header, and only contains the pairs in ''<key>=<value>'' format.

**Syntax**::

  Vocab(COLUMN_NAME, properties file URI)

* **COLUMN_NAME**: column name to get values from
* **properties file URI**: absolute path of the properties file or relative to the mapping file location

**Example:**

Properties file::

  1GRAV=urn:cgi:classifier:CGI:SimpleLithology:2008:gravel
  1TILL=urn:cgi:classifier:CGI:SimpleLithology:2008:diamictite
  6ALLU=urn:cgi:classifier:CGI:SimpleLithology:2008:sediment

Mapping file::

  <AttributeMapping>
    <targetAttribute>gml:name</targetAttribute>
    <sourceExpression>
        <OCQL>Vocab(ABBREVIATION, '/test-data/mapping.properties')</OCQL>
    </sourceExpression>
  </AttributeMapping>

The above example will map **gml:name** to *urn:cgi:classifier:CGI:SimpleLithology:2008:gravel* if ABBREVIATION value is *1GRAV*.