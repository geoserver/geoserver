..  _app-schema.cql-functions:


CQL functions
=============


CQL functions enable data conversion and conditional behaviour to be specified in mapping files. Some of these functions are provided by the app-schema plugin specifically for this purpose.

* The uDig manual includes a list of CQL functions:

    * http://udig.refractions.net/confluence/display/EN/Constraint%20Query%20Language.html

* CQL string literals are enclosed in single quotes, for example ``'urn:ogc:def:nil:OGC:missing'``.
* Single quotes are represented in CQL string literals as two single quotes, just as in SQL. For example, ``'yyyy-MM-dd''T''HH:mm:ss''Z'''`` for the string ``yyyy-MM-dd'T'HH:mm:ss'Z'``.


Vocabulary translation
----------------------


This section describes how to serve vocabulary translations using some function expressions in application schema mapping file.
If you're not familiar with application schema mapping file, read :ref:`app-schema.mapping-file`.


Recode
``````


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


Categorize
``````````


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


Vocab
`````


This function is more useful for bigger vocabulary pairs.
Instead of writing a long key-to-value pairs in the function, you can keep them in a separate properties file.
The properties file serves as a lookup table to the function. It has no header, and only contains the pairs in ''<key>=<value>'' format.

**Syntax**::

  Vocab(COLUMN_NAME, properties file)

* **COLUMN_NAME**: column name to get values from
* **properties file**: absolute path of the properties file

**Example:**

Properties file::

  1GRAV=urn:cgi:classifier:CGI:SimpleLithology:2008:gravel
  1TILL=urn:cgi:classifier:CGI:SimpleLithology:2008:diamictite
  6ALLU=urn:cgi:classifier:CGI:SimpleLithology:2008:sediment

Mapping file::

  <AttributeMapping>
    <targetAttribute>gml:name</targetAttribute>
    <sourceExpression>
        <OCQL>Vocab(ABBREVIATION, strconcat('${config.parent}', '/mapping.properties'))</OCQL>
    </sourceExpression>
  </AttributeMapping>

The above example will map **gml:name** to *urn:cgi:classifier:CGI:SimpleLithology:2008:gravel* if ABBREVIATION value is *1GRAV*.

This example uses the ``config.parent`` predefined interpolation property to specify a vocabulary properties file in the same directory as the mapping file. See :ref:`app-schema.property-interpolation` for details.


Geometry creation
-----------------


toDirectPosition
````````````````


This function converts double values to ``DirectPosition`` geometry type. This is needed when the data store doesn't have geometry type columns. This function expects:

Literal
    ``'SRS_NAME'`` (optional)
Expression
    expression of SRS name if ``'SRS_NAME'`` is present as the first argument
Expression
    name of column pointing to first double value
Expression
    name of column pointing to second double value (optional, only for 2D)


ToEnvelope
``````````


``ToEnvelope`` function can take in the following set of parameters and return as either ``Envelope`` or ``ReferencedEnvelope`` type:

**Option 1 (1D Envelope)**::

    ToEnvelope(minx,maxx)

**Option 2 (1D Envelope with crsname)**::

    ToEnvelope(minx,maxx,crsname)

**Option 3 (2D Envelope)**::

    ToEnvelope(minx,maxx,miny,maxy)
    
**Option 4 (2D Envelope with crsname)**::

    ToEnvelope(minx,maxx,miny,maxy,crsname)


toPoint
```````


This function converts double values to a 2D Point geometry type. This is needed when the data store doesn't have geometry type columns. This function expects:

Literal
    ``'SRS_NAME'`` (optional)
Expression
    expression of SRS name if ``'SRS_NAME'`` is present as the first argument
Expression
    name of column pointing to first double value
Expression
    name of column pointing to second double value
Expression
    expression of gml:id (optional)
    
toLineString
````````````

This function converts double values to 1D LineString geometry type. This is needed to express 1D borehole intervals with custom (non EPSG) CRS.

Literal
    ``'SRS_NAME'`` (EPSG code or custom SRS)
Expression
    name of column pointing to first double value
Expression
    name of column pointing to second double value


Reference
---------


toXlinkHref
```````````


This function redirects an attribute to be encoded as xlink:href, instead of being encoded as a full attribute. This is useful in polymorphism, where static client property cannot be used when the encoding is conditional. This function expects:

Expression
    REFERENCE_VALUE (could be another function or literal)



Date/time formatting
--------------------


FormatDateTimezone
``````````````````


A function to format a date/time using a `SimpleDateFormat pattern <https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html>`_ in a `time zone supported by Java <http://joda-time.sourceforge.net/timezones.html>`_. This function improves on ``dateFormat``, which formats date/time in the server time zone and can produce unintended results. Note that the term "date" is derived from a Java class name; this class represents a date/time, not just a single day.

**Syntax**::

    FormatDateTimezone(pattern, date, timezone)

pattern
    formatting pattern supported by `SimpleDateFormat <http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html>`_, for example ``'yyyy-MM-dd'``. Use two single quotes to include a literal single quote in a CQL string literal, for example ``'yyyy-MM-dd''T''HH:mm:ss''Z'''``.
date
    the date/time to be formatted or its string representation, for example ``'1948-01-01T00:00:00Z'``. An exception will be returned if the date is malformed (and not null). Database types with time zone information are recommended.
timezone
    the name of a time zone supported by Java, for example ``'UTC'`` or ``'Canada/Mountain'``. Note that unrecognised timezones will silently be converted to UTC.

This function returns null if any parameter is null.

This example formats date/times from a column ``POSITION`` in UTC for inclusion in a ``csml:TimeSeries``::

    <AttributeMapping>
        <targetAttribute>csml:timePositionList</targetAttribute>                    
        <sourceExpression>
            <OCQL>FormatDateTimezone('yyyy-MM-dd''T''HH:mm:ss''Z''', POSITION, 'UTC')</OCQL>
        </sourceExpression>
        <isList>true</isList>
    </AttributeMapping>

