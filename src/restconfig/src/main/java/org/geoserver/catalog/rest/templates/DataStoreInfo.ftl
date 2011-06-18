<#include "head.ftl">
Data Store "${properties.name}"
<ul>
<#list properties.featureTypes as ft>
  <li><a href="${page.pageURI('/featuretypes/' + ft.properties.name + '.html')}">${ft.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">
