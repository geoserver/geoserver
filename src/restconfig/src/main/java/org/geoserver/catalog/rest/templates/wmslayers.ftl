<#include "head.ftl">
WMS layers:
<ul>
<#list values as wml>
  <li><a href="${page.pageURI('/' + wml.properties.name + '.html')}">${wml.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">