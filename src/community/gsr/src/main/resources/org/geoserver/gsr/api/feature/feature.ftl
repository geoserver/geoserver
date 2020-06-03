<#include "common-header.ftl">
   <#include "breadcrumbs.ftl">
   <h2>${model.workspace} (FeatureServer)</h2>
   <!-- View In: TODO?-->
   <p><b>Service Description: </b>${model.serviceDescription}</p>
   <p><b>Layers: </b></p>
   <ul>
   <#list model.layers as layer>
   <li><a href="${serviceLink("${model.workspace}/FeatureServer/${layer.id}")}">${layer.name}</a> (${layer.id})</li>
   </#list>
   </ul>
   <p><b>Tables: </b></p>
   <!-- TODO?-->
   <#include "interfaces.ftl">
<#include "common-footer.ftl">