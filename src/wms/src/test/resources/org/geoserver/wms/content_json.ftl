<#list features as feature>
{
"content" : "this is the content",
"type": "Feature",
"id" : "${feature.fid}",
"geometry_name":"the_geom",
<#list feature.attributes as attribute1>
<#if attribute1.isGeometry>
"geometry": "${attribute1.value}",
</#if>
</#list>
"properties": {
<#list feature.attributes as attribute>
<#if !attribute.isGeometry>
"${attribute.name}": "${attribute.value}"
</#if>
<#if attribute_has_next && !attribute.isGeometry>
,
</#if>
</#list>
}
}
<#if feature_has_next>
,
</#if>
</#list>