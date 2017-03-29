<#include "head.ftl">
Workspace "${properties.name}"
<ul>
<#if properties.dataStores??>
<#list properties.dataStores as s>
<li><a href="${page.pageURI('/datastores/' + s.properties.name + '.html')}">${s.properties.name}</a></li>
</#list>
</#if>
<#if properties.coverageStores??>
<#list properties.coverageStores as s>
  <li><a href="${page.pageURI('/coveragestores/' + s.properties.name + '.html')}">${s.properties.name}</a></li>
</#list>
</#if>
<#if properties.wmsStores??>
<#list properties.wmsStores as s>
  <li><a href="${page.pageURI('/wmsstores/' + s.properties.name + '.html')}">${s.properties.name}</a></li>
</#list>
</#if>
</ul>
<#include "tail.ftl">
