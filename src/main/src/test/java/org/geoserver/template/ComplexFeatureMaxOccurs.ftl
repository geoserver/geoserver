<#macro property prop>
<#if prop.isComplex>
<@feature node=prop.rawValue/>
<#else>
Name: ${prop.name}
Value: ${prop.value?string}
</#if>
</#macro>

<#macro feature node>
      <#list node.attributes as attribute>
      <@property prop=attribute />
      </#list>
</#macro>

<#list attributes as attr>
<@property prop=attr />
</#list>