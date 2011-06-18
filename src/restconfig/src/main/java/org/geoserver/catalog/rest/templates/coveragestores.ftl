<#include "head.ftl">
Coverage Stores:
<ul>
<#list values as cs>
  <li><a href="${page.pageURI(cs.properties.name + '.html')}">${cs.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">
