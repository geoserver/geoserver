<#include "head.ftl">
Url check "${properties.name}"

<ul>
  <li>Name: ${properties.name}</li>
  <li>Description: ${properties.description}</li>
  <li>Configuration: ${properties.configuration}</li>
  <li>Enabled: ${properties.enabled}</li>
</ul>

<#include "tail.ftl">