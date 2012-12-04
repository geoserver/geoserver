<?xml version="1.0" encoding="UTF-8"?>
<!--
    SWE Common is an OGC Standard.
    Copyright (c) 2010 Open Geospatial Consortium. 
    To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
-->
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
    <sch:title>Additional validation rules for XML instances including simple data components</sch:title>
    <sch:ns uri="http://www.opengis.net/swe/2.0" prefix="swe"/>
    <sch:ns uri="http://www.w3.org/1999/xlink" prefix="xlink"/>
    <sch:pattern>
        <sch:title>Req 18</sch:title>
        <sch:rule context="//swe:Quantity | //swe:Count[not(parent::swe:elementCount)] | //swe:Time | //swe:Boolean | //swe:Category | //swe:Text">
            <sch:assert test="@definition">
                The 'definition' attribute is mandatory on all simple data components (Req 18)
            </sch:assert>
            <sch:report test="not(starts-with(@definition, 'http://'))">
                The 'definition' attribute is not a HTTP URL. Is is really resolvable?
            </sch:report>
        </sch:rule>
    </sch:pattern>
    <sch:pattern>
        <sch:title>Req 18</sch:title>
        <sch:rule context="//swe:QuantityRange | //swe:CountRange | //swe:TimeRange | //swe:CategoryRange">
            <sch:assert test="@definition">
                The 'definition' attribute is mandatory on all range data components (Req 18)
            </sch:assert>
            <sch:report test="not(starts-with(@definition, 'http://'))">
                The 'definition' attribute is not a HTTP URL. Is is really resolvable?
            </sch:report>
        </sch:rule>
    </sch:pattern>
    <sch:pattern>
        <sch:title>Req 25</sch:title>
        <sch:rule context="//swe:Category[not(parent::swe:choiceValue)]">
            <sch:assert test="swe:codeSpace | swe:constraint">
                A 'Category' component must have either a code space or an enumeration constraint (Req 25)
            </sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern>
        <sch:title>Req 29</sch:title>
        <sch:rule context="//swe:Time">
            <sch:report test="@referenceFrame = @localFrame">
                The 'referenceFrame' and 'localFrame' attributes shall have different values (Req 29)
            </sch:report>
        </sch:rule>
    </sch:pattern>
    <sch:pattern>
        <sch:title>Req 58</sch:title>
        <!-- name of property elements always starts with a lower case character! -->
        <sch:rule context="//swe:*[matches(local-name(), '^[a-z].*$')]">
            <sch:assert test="@xlink:href | @code | * or (normalize-space(.) != '')">
                A property element shall have children or an xlink:href (Req 58)
            </sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern>
        <sch:title>Req 65</sch:title>
        <sch:rule context="//swe:uom">
            <sch:assert test="@code | @xlink:href">
                Either a UCUM code or a URI pointing to a non UCUM unit shall be specified (Req 65)
            </sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern>
        <sch:title>Req 66</sch:title>
        <sch:rule context="//swe:Time[matches(swe:value, '([T:]|([0-9]-))')] | //swe:TimeRange[matches(swe:value, '([T:]|([0-9]-))')]">
            <sch:assert test="swe:uom/@xlink:href = 'http://www.opengis.net/def/uom/ISO-8601/0/Gregorian'">
                ISO8601 shall be specified as the uom when the time value is ISO8601 encoded (Req 66)
            </sch:assert>
        </sch:rule>
    </sch:pattern>    
</sch:schema>