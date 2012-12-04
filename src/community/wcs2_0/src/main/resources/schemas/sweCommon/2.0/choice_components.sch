<?xml version="1.0" encoding="UTF-8"?>
<!--
    SWE Common is an OGC Standard.
    Copyright (c) 2010 Open Geospatial Consortium. 
    To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
-->
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
    <sch:title>Additional validation rules for XML instances including choice data components</sch:title>
    <sch:ns uri="http://www.opengis.net/swe/2.0" prefix="swe"/>
    <sch:pattern>
        <sch:title>Req 46</sch:title>
        <sch:rule context="//swe:DataChoice/swe:item">
            <sch:assert test="not(@name = preceding-sibling::swe:item/@name)">
                Item names shall be unique within the 'DataChoice' component (Req 46)
            </sch:assert>
        </sch:rule>
    </sch:pattern>
</sch:schema>