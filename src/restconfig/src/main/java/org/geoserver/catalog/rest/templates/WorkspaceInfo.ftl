<#include "head.ftl">
Workspace "${properties.name}"
<ul>
<#list properties.dataStores as s>
<li><a href="${page.pageURI('/datastores/' + s.properties.name + '.html')}">${s.properties.name}</a></li>
</#list>
<#list properties.coverageStores as s>
  <li><a href="${page.pageURI('/coveragestores/' + s.properties.name + '.html')}">${s.properties.name}</a></li>
</#list>
<#list properties.wmsStores as s>
  <li><a href="${page.pageURI('/wmsstores/' + s.properties.name + '.html')}">${s.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">
