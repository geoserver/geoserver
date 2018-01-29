<#include "head.ftl">
Layer group "${properties.name}"

<ul>
<h2>Layers</h2>
<#list properties.layers as l>
  <li><#if l.properties.prefixedName??><a href="${page.servletURI('/layers/' + l.properties.prefixedName + '.html')}">${l.properties.prefixedName}</a></#if></li>
</#list>
<h2>Styles</h2>
<#list properties.styles as s>
  <#if s.properties.workspace??>
    <li><a href="${page.servletURI('/workspaces/'+ s.properties.workspace +'/styles/' + s.properties.name + '.html')}">${s.properties.workspace}:${s.properties.name}</a></li>
  <#else>
    <li><#if s.properties.name??><a href="${page.servletURI('/styles/' + s.properties.name + '.html')}">${s.properties.name}</a></#if></li>
  </#if>
</#list>
</ul>

<#include "tail.ftl">