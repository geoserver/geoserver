<#include "common-header.ftl">
<h2>${model.id}</h2>
<ul>

Formats available and url templates to access tiles:
<ul>
<#list model.links as link>
<#if link.rel == 'tile'>
<li>${link.type}: <code>${link.href}</code></li> 
</#if>
</#list>
</ul>

Tile matrix sets in which the above URLs are available, and grid limits:

<ul>
<#list model.tileMatrixSetLinks as tms>
<li><a href="${tms.tileMatrixSetURI}"$>${tms.tileMatrixSet}</a>
<table style="width:auto">
  <tr><th>tileMatrixSetId</th><th>minRow</th><th>maxRow</th><th>minCol</th><th>maxCol</th>
  <#list tms.tileMatrixSetLimits as limit>
    <tr><td>${limit.tileMatrix}</td><td>${limit.minTileRow}</td><td>${limit.maxTileRow}</td><td>${limit.maxTileCol}</td><td>${limit.maxTileCol}</td>
  </#list>
</table>
</li>
</#list>
</ul>

<#include "common-footer.ftl">
