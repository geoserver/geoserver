<#macro geometry(geometry)>
<b>${geometry.geometryType}: </b><br/>
<#if geometry.geometryType == "POLYGON">
<#assign i = 0>
<#list geometry.rings as ring>
Ring${i}: 
<#list 1..3 as point>
    [${ring[point][0]}, ${ring[point][1]}]
</#list>
... ${ring?size - 3} more ...
<#assign i = i + 1>
</#list>
</#if>
</#macro>