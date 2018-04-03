<#include "head.ftl">
Coverage Store "${properties.name}"
<ul>
<#list properties.coverages as coverage>
  <li><a href="${page.pageURI('/coverages/' + coverage.properties.name + '.html')}">${coverage.properties.name}</a></li>
</#list>
</ul>
<#include "tail.ftl">