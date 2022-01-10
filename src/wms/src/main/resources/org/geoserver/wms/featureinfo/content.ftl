<#-- 
Body section of the GetFeatureInfo template, it's provided with one feature collection, and
will be called multiple times if there are various feature collections
-->
<table class="featureInfo">
  <caption class="featureInfo">${type.name}</caption>
  <tr>
  <th>fid</th>
<#list type.attributes as attribute>
  <#if !attribute.isGeometry>
    <th >${attribute.name}</th>
  </#if>
</#list>
  </tr>

<#assign odd = false>
<#list features as feature>
  <#if odd>
    <tr class="odd">
  <#else>
    <tr>
  </#if>
  <#assign odd = !odd>

  <td>${feature.fid}</td>    
  <#list feature.attributes as attribute>
    <#if !attribute.isGeometry>
      <td>${attribute.value?string}</td>
    </#if>
  </#list>
  </tr>
</#list>
</table>
<br/>

