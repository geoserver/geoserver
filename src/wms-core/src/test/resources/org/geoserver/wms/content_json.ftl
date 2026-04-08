<#list features as feature>
{
"content" : "this is the content",
"type": "Feature",
"id" : "${feature.fid}",
<#list feature.attributes as attribute>
<#if attribute.isGeometry>
"geometry": ${geoJSON.geomToGeoJSON(attribute.rawValue)},
</#if>
</#list>
"properties": {
<#list feature.attributes as attribute2>
<#if !attribute2.isGeometry>
"${attribute2.name}": "${attribute2.value}"
</#if>
<#if attribute2_has_next && !attribute2.isGeometry>
,
</#if>
</#list>
}
}
<#if feature_has_next>
,
</#if>
</#list>