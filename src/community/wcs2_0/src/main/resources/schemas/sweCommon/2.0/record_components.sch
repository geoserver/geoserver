<?xml version="1.0" encoding="UTF-8"?>
<!--
    SWE Common is an OGC Standard.
    Copyright (c) 2010 Open Geospatial Consortium. 
    To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
-->
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
    <sch:title>Additional validation rules for XML instances including record data components</sch:title>
    <sch:ns uri="http://www.opengis.net/swe/2.0" prefix="swe"/>
    <sch:pattern>
        <sch:title>Req 39</sch:title>
        <sch:rule context="//swe:DataRecord/swe:field">
            <sch:assert test="not(@name = preceding-sibling::swe:field/@name)">
                Field names shall be unique within a 'DataRecord' component (Req 39)
            </sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern>
        <sch:title>Req 40</sch:title>
        <sch:rule context="//swe:Vector/swe:coordinate">
            <sch:assert test="not(@name = preceding-sibling::swe:coordinate/@name)">
                Coordinate names shall be unique within a 'Vector' component (Req 40)
            </sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern>
        <sch:title>Req 41, 42, 43</sch:title>
        <sch:rule context="//swe:Vector">
            <sch:report test="swe:coordinate/*/@referenceFrame">
                The 'referenceFrame' attribute is forbidden on vector coordinates (Req 41)
            </sch:report>
            <sch:assert test="swe:coordinate/*/@axisID">
                The 'axisID' attribute is mandatory on vector coordinates (Req 42)
            </sch:assert>
            <sch:report test="@referenceFrame = @localFrame">
                The 'referenceFrame' and 'localFrame' attributes shall have different values (Req 43)
            </sch:report>
        </sch:rule>
    </sch:pattern>
</sch:schema>