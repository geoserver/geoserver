<#include "head.ftl">
WMS Layer "${properties.name}"

<ul>
  <li>Name: ${properties.name}</li>
  <li>Description: ${properties.description}</li>
  <li>Abstract: ${properties.abstract}</li>
  <li>Enabled: ${properties.enabled}</li>
  <li>SRS: ${properties.sRS}</li>
  <li>Bounds: ${properties.boundingBox}></li> 
</ul>
  
<#include "tail.ftl">
