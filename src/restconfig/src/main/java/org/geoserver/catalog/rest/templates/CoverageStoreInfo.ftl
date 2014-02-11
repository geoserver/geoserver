<#include "head.ftl">
Coverage Store "${properties.name}"
<ul>
<#list properties.coverages as c>
  <li><a href="${page.pageURI('coverages/' + c.properties.name + '.html')}">${c.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">