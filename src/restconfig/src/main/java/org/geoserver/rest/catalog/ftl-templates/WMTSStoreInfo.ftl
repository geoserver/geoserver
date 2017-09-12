<#include "head.ftl">
WMTS Store "${properties.name}"
<ul>
<#list properties.layers as wml>
  <li><a href="${page.pageURI('/layers/' + wml.properties.name + '.html')}">${wml.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">
