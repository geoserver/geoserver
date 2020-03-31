<#include "common-header.ftl">
<#include "breadcrumbs.ftl">
<#include "geometry.ftl">
<h2>Feature: (ID: ${model.feature.id})</h2>
<p><b>Attributes: </b></p>
<ul>
<#list model.feature.attributes?keys as key> 
   <li>${key}: ${model.feature.attributes[key]}</li>
</#list> 
</ul>
<p>
<#if model.feature.geometry??>
<#call geometry(model.feature.geometry)>
</#if>
</p>
<#include "interfaces.ftl">
<p><b>Child resources: </b><!-- TODO --></p>
<#include "operations.ftl">
<#include "common-footer.ftl">