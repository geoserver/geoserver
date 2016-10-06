<?xml version="1.0" encoding="UTF-8"?>
<!--
    SWE Common is an OGC Standard.
    Copyright (c) 2010 Open Geospatial Consortium. 
    To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
-->
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
    <sch:title>Additional validation rules for XML instances including block data components</sch:title>
    <sch:ns uri="http://www.opengis.net/swe/2.0" prefix="swe"/>
    <sch:pattern>
        <sch:title>Req 49</sch:title>
        <sch:rule context="//swe:DataArray/swe:elementType//swe:value | //swe:Matrix/swe:elementType//swe:value | //swe:DataStream/swe:elementType//swe:value">
            <sch:assert test="ancestor::swe:elementCount">
                Components that are children of a block component shall never have inline values (Req 49)
            </sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern>
        <sch:title>Req 50</sch:title>
        <sch:rule context="//swe:DataArray/swe:values | //swe:Matrix/swe:values | //swe:DataStream/swe:values">
            <sch:assert test="parent::*/swe:encoding">
                Block components containing block encoded values shall also specify an encoding method (Req 50)
            </sch:assert>
        </sch:rule>
    </sch:pattern>
  <sch:pattern>
      <sch:title>Req 51</sch:title>
      <sch:rule context="//swe:Matrix/swe:elementType[*]">
          <sch:assert test="swe:Matrix | swe:Count | swe:Quantity | swe:Time">
              The element type of a Matrix shall be a nested Matrix or a scalar numerical component (Req 51)
          </sch:assert>
      </sch:rule>
  </sch:pattern>
</sch:schema>