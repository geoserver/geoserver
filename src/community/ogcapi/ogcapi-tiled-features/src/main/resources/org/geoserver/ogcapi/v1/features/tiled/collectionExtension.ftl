<#assign link=collection.getLinkUrl('tilesets-vector', 'text/html')>
<#if link??>
<li>
<a href="${link}" id="${collection.htmlId}_tiles">Data tiles</a> 
</li>
</#if>