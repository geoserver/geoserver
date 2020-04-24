<#include "common-header.ftl">
<#include "breadcrumbs.ftl">
<#include "layer.ftl">
    <p><b>Layers:</b></p>
   <#list model.layers as layer>
   <#call layerDescription(layer, "Feature Layer")>
   </#list>
   <p><b>Tables:</b></p><!-- TODO ??? -->
   <#include "interfaces.ftl">
<#include "common-footer.ftl">
