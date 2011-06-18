<#include "head.ftl">
Data Stores:
<ul>
<#list values as ds>
  <li><a href="${page.pageURI(ds.properties.name + '.html')}">${ds.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">
