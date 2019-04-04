<#include "head.ftl">
Workspace "${properties.name}"
<ul>
<#if properties.dataStores??>
<h2>DataStores</h2>
<#list properties.dataStores as s>
<li><a href="${page.pageURI('/datastores/' + s.properties.name + '.html')}">${s.properties.name}</a></li>
</#list>
</#if>
<#if properties.coverageStores??>
<h2>CoverageStores</h2>
<#list properties.coverageStores as s>
  <li><a href="${page.pageURI('/coveragestores/' + s.properties.name + '.html')}">${s.properties.name}</a></li>
</#list>
</#if>
<#if properties.wmsStores??>
<h2>WMSStores</h2>
<#list properties.wmsStores as s>
  <li><a href="${page.pageURI('/wmsstores/' + s.properties.name + '.html')}">${s.properties.name}</a></li>
</#list>
</#if>
<#if properties.wmtsStores??>
<h2>WMTSStores</h2>
<#list properties.wmtsStores as s>
  <li><a href="${page.pageURI('/wmtsstores/' + s.properties.name + '.html')}">${s.properties.name}</a></li>
</#list>
</#if>
</ul>
<#include "tail.ftl">
