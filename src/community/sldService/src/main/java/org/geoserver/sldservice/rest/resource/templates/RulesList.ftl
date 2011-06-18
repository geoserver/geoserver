<#include "head.ftl">
  Rules List for Layer: "${properties.layerName}"

  <br/>
  Rules:
  <ul>
  <#list properties.rules as p>
  	<li>${p}
  </#list>
  </ul>
  
<#include "tail.ftl">