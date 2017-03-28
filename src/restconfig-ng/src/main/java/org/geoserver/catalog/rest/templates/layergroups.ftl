<#include "head.ftl">
Layers:
<ul>
<#list values as lg>
  <li><a href="${page.pageURI(lg.properties.name + '.html')}">${lg.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">