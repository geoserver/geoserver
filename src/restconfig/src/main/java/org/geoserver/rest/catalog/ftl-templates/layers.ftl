<#include "head.ftl">
Layers:
<ul>
<#list values as l>
  <li><a href="${page.pageURI(l.properties.prefixedName + '.html')}">${l.properties.prefixedName}</a></li>
</#list>
</ul>
<#include "tail.ftl">