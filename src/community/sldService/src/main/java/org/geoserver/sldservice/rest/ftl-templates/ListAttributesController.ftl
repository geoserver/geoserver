<#include "head.ftl">
  Attributes List for Layer: "${properties.layerName}"

  <br/>
  Attributes:
  <ul>
  <#list properties.attributes.keySet() as p>
  	<li>${p}:${properties.attributes.get(p)}
  </#list>
  </ul>
  
<#include "tail.ftl">