<#include "head.ftl">
Layers:
<ul>
<#list values as l>
  <li><a href="${page.pageURI(l.properties.name + '.html')}">${l.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">