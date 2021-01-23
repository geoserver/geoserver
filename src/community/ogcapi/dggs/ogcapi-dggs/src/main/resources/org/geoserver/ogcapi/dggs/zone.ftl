<#include "common-header.ftl">

   <#assign feature = model.response.features?first.features?first>
   <h1>Zone: ${feature.zoneId.value}</h1>    
   <ul>
    <#list feature.attributes as attribute>
      <#if !attribute.isGeometry && attribute.name != 'zoneId'>
        <li>${attribute.name}: ${attribute.value}</li>
      </#if>
    </#list>
   </ul>

   Access zone relatives:  
   <ul>
     <li><a href="${model.getLinkUrl('parents', 'text/html')!}">Zone parents</a></li>
     <li><a href="${model.getLinkUrl('children', 'text/html')!}">Zone children</a></li>
     <li><a href="${model.getLinkUrl('neighbors', 'text/html')!}">Zone neighbor</a></li>
   </ul>
   
       
<#include "common-footer.ftl">
