<#include "head.ftl">
Workspaces
<ul>
<#list values as w>
  <li><a href="${page.pageURI(w.properties.name + '.html')}">${w.properties.name}</a><#if w.properties.isDefault> [default] </#if></li>
</#list>
</ul>
<#include "tail.ftl">
