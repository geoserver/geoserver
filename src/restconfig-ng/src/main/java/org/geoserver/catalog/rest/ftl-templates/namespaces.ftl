<#include "head.ftl">
Namespaces
<ul>
<#list values as n>
  <li><a href="${page.pageURI("/" + n.properties.prefix + '.html')}">${n.properties.prefix}</a><#if n.properties.isDefault> [default] </#if></li>
</#list>
</ul>
<#include "tail.ftl">