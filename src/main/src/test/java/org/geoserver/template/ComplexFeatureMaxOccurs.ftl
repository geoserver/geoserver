<#macro property prop>
<#if prop?is_enumerable>
<#list prop as elem>
<@property prop=elem/>
</#list>
<#elseif prop.isComplex>
<@feature node=prop.rawValue/>
<#else>
<#if !prop.name?contains("FEATURE_LINK")>
Name: ${prop.name}
Value: ${prop.value?string}
</#if>
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