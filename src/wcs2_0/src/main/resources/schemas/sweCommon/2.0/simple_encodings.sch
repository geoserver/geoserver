<?xml version="1.0" encoding="UTF-8"?>
<!--
    SWE Common is an OGC Standard.
    Copyright (c) 2010 Open Geospatial Consortium. 
    To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
-->
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
    <sch:title>Additional validation rules for XML instances including simple encodings</sch:title>
    <sch:ns uri="http://www.opengis.net/swe/2.0" prefix="swe"/>
    <sch:ns uri="http://www.w3.org/1999/xlink" prefix="xlink"/>
    <sch:pattern>
        <sch:title>Req 76</sch:title>
        <sch:rule context="//*[swe:encoding/swe:TextEncoding]/swe:values">
          <sch:assert test="@xlink:href or (normalize-space(.) != '')">
                When text encoding is specified, values shall contain text content (Req 76)
            </sch:assert>
            <sch:assert test="not(*)">
                When text encoding is used, the values element shall not contain sub-elements (Req 76)
            </sch:assert>
        </sch:rule>
    </sch:pattern>  
    <sch:pattern>
        <sch:title>Req 77</sch:title>
        <sch:rule context="//*[swe:encoding/swe:XMLEncoding]/swe:values">
          <sch:assert test="@xlink:href | *">
              When XML encoding is specified, values shall contain XML elements (Req 77)
          </sch:assert>
        </sch:rule>
    </sch:pattern>
</sch:schema>