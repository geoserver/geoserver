<#include "head.ftl">
	
<ul>
<#list values as t>
  <li><a href="${page.pageURI(t.properties.name)}">${t.properties.name}</a></li>
</#list>
</ul>
	
<#include "tail.ftl">