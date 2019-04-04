<#include "head.ftl">
Feature Types:
<ul>
<#list values as ft>
  <li><a href="${page.pageURI('/' + ft.properties.name + '.html')}">${ft.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">