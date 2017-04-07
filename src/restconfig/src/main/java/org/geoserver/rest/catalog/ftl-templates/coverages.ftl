<#include "head.ftl">
Coverages:
<ul>
<#list values as c>
  <li><a href="${page.pageURI(c.properties.name + '.html')}">${c.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">
