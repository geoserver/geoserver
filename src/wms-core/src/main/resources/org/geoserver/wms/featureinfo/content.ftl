<#-- 
Body section of the GetFeatureInfo template, it's provided with one feature collection, and
will be called multiple times if there are various feature collections
-->
<#list features as feature>
<table class="featureInfo">
  <caption class="featureInfo">${type.name}</caption>
<tbody>
<tr>
  <th scope="row">FeatureID</th>
  <td>${feature.fid}</td>
</tr>
<#list feature.attributes as attribute>
<#if !attribute.isGeometry && !attribute.value.isEmpty() >
<#assign odd = false>
<#if odd>  
  <tr class="odd">
<#else>
  <tr>
</#if> 
    <th scope="row">${attribute.name}</th>
    <td>${attribute.value}</td>
  </tr>
</#if>
</#list>
</tbody>
</table>
<br/>
</#list>

