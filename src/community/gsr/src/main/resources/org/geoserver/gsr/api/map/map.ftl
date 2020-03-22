<#include "common-header.ftl">
   <#include "breadcrumbs.ftl">
   <h2>${model.workspace} (MapServer)</h2>
   <!-- View In: TODO?-->
   <p><b>Service Description: </b>${model.mapName}</p>
   <p><b>Map Name: </b>${model.mapName}</p>
   <p>Legend</p><!-- TODO -->
   <p><a href="${serviceLink("${model.workspace}/MapServer/layers")}">All Layers and Tables</a></p>
   <p><b>Layers: </b></p>
   <ul>
   <#list model.layers as layer>
   <li><a href="${serviceLink("${model.workspace}/MapServer/${layer.id}")}">${layer.name}</a> (${layer.id})</li>
   </#list>
   </ul>
   <p><b>Tables: </b></p><!-- TODO ?? -->
   <p><b>Description: </b><!-- TODO --></p>
   <p><b>Copyright Text: </b><!-- TODO --></p>
   <p><b>Spatial Reference: </b>${model.spatialReference.wkid?string('#####')}</p>
   <p><b>Single Fused Map Cache: </b>${model.singleFusedMapCache?string}</p>
   <p><b>Initial Extent: </b><!-- TODO --></p>
   <p><b>Full Extent: </b><!-- TODO --></p>
   <p><b>Units: </b><!-- TODO --></p>
   <p><b>Supported Image Format Types: </b><!-- TODO --></p>
   <p><b>Document Info: </b><!-- TODO --></p>
   <#include "interfaces.ftl">
<#include "common-footer.ftl">