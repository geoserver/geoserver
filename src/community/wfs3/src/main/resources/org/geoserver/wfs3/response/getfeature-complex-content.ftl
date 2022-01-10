<#-- 
Macro's used for content
-->

<#macro property node>
    <#if !node.isGeometry>
       <#if node.isComplex>      
       <td> <@feature node=node.rawValue type=node.type /> </td>  
      <#else>
      <td>${node.value?string}</td>
      </#if>
    </#if>
</#macro>

<#macro header typenode>
 <caption class="featureInfo">${typenode.name}</caption>
  <tr>
  <th>fid</th>
<#list typenode.attributes as attribute>
  <#if !attribute.isGeometry>
    <#if attribute.prefix == "">      
    	<th >${attribute.name}</th>
    <#else>
    	<th >${attribute.prefix}:${attribute.name}</th>
    </#if>
  </#if>
</#list>
  </tr>
</#macro>

<#macro feature node type>
 <table class="featureInfo">
  <@header typenode=type />
  <tr>
  <td>${node.fid}</td>    
  <#list node.attributes as attribute>
      <@property node=attribute />
  </#list>
  </tr>
</table>
</#macro>
  
<#-- 
Body section of the GetFeatureInfo template, it's provided with one feature collection, and
will be called multiple times if there are various feature collections
-->
<table class="featureInfo">
  <@header typenode=type />

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
    <@property node=attribute />
  </#list>
  </tr>
</#list>
</table>
<br/>

