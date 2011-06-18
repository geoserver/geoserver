<#include "head.ftl">
WMS Store "${properties.name}"
<ul>
<#list properties.wmsLayers as wml>
  <li><a href="${page.pageURI('/wmslayers/' + wml.properties.name + '.html')}">${wml.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">
