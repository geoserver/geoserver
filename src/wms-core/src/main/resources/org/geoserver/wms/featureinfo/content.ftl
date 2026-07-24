<#-- 
Body section of the GetFeatureInfo template, the template is provided with one feature collection at time,
in the event of multuple feature collections it will be called for for each feature collection in turn.
-->
<#list features as feature>
<table class="featureInfo">
  <caption class="featureInfo" <#if type.description??> title="${type.description} <#else/> title="${type.name}" </#if>>
    <#if type.title??> ${type.title} <#else/> ${type.name} </#if>
  </caption>
<tbody>
<tr class="odd">
  <th scope="row">FeatureID</th>
  <td>${feature.fid}</td>
</tr>
<#list feature.attributes as attribute>
<#if !attribute.isGeometry && !attribute.value.isEmpty() >
  <tr class="${attribute?item_parity}">
    <th<#if attribute.description??> title="${attribute.description}"</#if> scope="row">${attribute.name}</th>
    <td>${attribute.value}</td>
  </tr>
</#if>
</#list>
</tbody>
</table>
<br/>
</#list>