<#include "head.ftl">
Namespace "${properties.prefix}" (${properties.uRI})
<ul>
<#list properties.resources as r>
  <li>${r.properties.name}</li>
</#list>
</ul>
<#include "tail.ftl">