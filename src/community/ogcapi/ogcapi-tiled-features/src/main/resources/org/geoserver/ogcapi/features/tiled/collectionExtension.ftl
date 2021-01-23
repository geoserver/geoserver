<#assign link=collection.getLinkUrl('dataTiles', 'text/html')>
<#if link??>
<li>
<a href="${link}" id="${collection.htmlId}_tiles">Data tiles</a> 
</li>
</#if>