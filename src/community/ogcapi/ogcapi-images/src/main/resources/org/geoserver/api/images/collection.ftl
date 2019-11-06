<#include "common-header.ftl">
<h2>${model.id}</h2>
<ul>
<#if model.title??> 
<li><b>Title</b>: <span id="${model.htmlId}_title">${model.title}</span><br/></li>
</#if>
<#if model.description??>
<li><b>Description</b>: <span id="${model.htmlId}_description">${model.description!}</span><br/></li>
</#if>
<#assign spatial = model.extent.spatial>
<li><b>Geographic extents</b>:
<ul>
<#list spatial as se>
<li>${se.getMinX()}, ${se.getMinY()}, ${se.getMaxX()}, ${se.getMaxY()}.</li>
</#list>
</ul>
</li>
</ul>

<a href=""${model.getLinkUrl('images', 'text/html')!}"">Images available for this collection.</a>

<#include "common-footer.ftl">
