<#include "symbol.ftl">
<#include "renderer.ftl">
<#include "field.ftl">
<#macro layerDescription(model, title)>
<h2>${title}: ${model.name} (ID: ${model.id})</h2>
<!-- View In: TODO?-->
<p><b>Display Field: </b></p><!-- ?? -->
<p><b>Type: </b>FeatureLayer</p>
<p><b>Geometry Type: </b>${model.geometryType}</p>
<p><b>Description: </b>${model.description}</p>
<p><b>Copyright Text: </b>${model.copyrightText}</p>
<p><b>Min Scale: </b>${model.minScale}</p>
<p><b>Max Scale: </b>${model.maxScale}</p>
<p><b>Default Visibility: </b>${model.defaultVisibility?string}</p>
<p><b>Extent:</b></p>
<ul>
    <li>XMin: ${model.extent.xmin}</li>
    <li>YMin: ${model.extent.ymin}</li>
    <li>XMax: ${model.extent.xmax}</li>
    <li>YMax: ${model.extent.ymax}</li>
    <li>Spatial Reference: ${model.extent.spatialReference.wkid?string('#####')}</li>
</ul>
<p><b>Drawing Info: </b></p>
<ul>
    <#call renderer(model.drawingInfo.renderer)>
    <li>Transparency: 0</li>
    <li>Labelling Info:
    <ul>
    <#if model.drawingInfo.labelingInfo??>
    <#list model.drawingInfo.labelingInfo as label>
        <li>Label Placement:</li>
        <li>Label Expression: ${label.labelExpression}</li>
        <li>Use Coded Values: ${label.useCodedValues?string}</li>
        <#call symbol(label.symbol)>
        <li>Min Scale: ${label.minScale}</li>
        <li>Max Scale: ${label.maxScale}</li>
    </#list>
    </#if>
    </ul>
    </li>
</ul>
<p><b>Has Attachments: </b>${model.hasAttachments?string}</p>
<p><b>HTML Popup Type: </b>${model.htmlPopupType?if_exists}</p>
<p><b>Object ID Field: </b>${model.objectIdField?if_exists}</p>
<p><b>Global ID Field: </b>${model.globalIdField?if_exists}</p>
<p><b>Type ID Field: </b><!-- TODO ? --></p>
<p><b>Fields: </b>
<ul>
<#list model.fields as field>
    <#call fieldDescription(field)>
</#list>
</ul>
</p>
<p><b>Types: </b> <!-- TODO -->
<#list model.types as type>
    <#call typeDescription(type)>
</#list>
</p>
</#macro>