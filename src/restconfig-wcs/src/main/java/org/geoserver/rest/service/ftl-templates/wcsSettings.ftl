<#include "head.ftl">

<#if properties.workspaceName??>
    Workspace Name:  "${properties.workspaceName!}"
</#if>


<h4>Service Metadata</h4>

<ul>
  <li>WCS Enabled:  "${properties.enabled!}"</li>
  <li>Strict CITE compliance:  "${properties.citeCompliant!}"</li>
  <li>Maintainer:  "${properties.maintainer!}"</li>
  <li>Online Resource:  "${properties.onlineResource!}"</li>
  <li>Title:  "${properties.title!}"</li>
  <li>Abstract:  "${properties.abstract!}"</li>
  <li>Fees:  "${properties.fees!}"</li>
  <li>Access Constraints:  "${properties.accessConstraints!}"</li>
  <#if properties.keywords?is_enumerable>
    <li>Keywords: "
        <#list properties.keywords as el>
            ${el.value!}
            <#if el.language??>(${el.language!}) </#if>
            <#if el.vocabulary??>[${el.vocabulary!}] </#if>
        <#if el_has_next>, </#if></#list>
    "</li>
  <#else>
    <li>Keywords: ""</li>
  </#if>
  <li>Name:  "${properties.name!}"</li>
  <#if properties.versions?is_enumerable>
    <li>Versions:  "${properties.versions!}"</li>
  <#else>
    <li>Versions: ""</li>
  </#if>
  <li>Schema Base URL:  "${properties.schemaBaseURL!}"</li>
  <li>Verbose Messages:  "${properties.verbose!}"</li>
</ul>

<h4>Coverage processing</h4>

<ul>
  <li>Use subsampling:  "${properties.isSubsamplingEnabled!}"</li>
  <li>Overview policy:  "${properties.overviewPolicy!}"</li>
</ul>

<h4>Resource Consumption Limits</h4>

<ul>
  <li>Maximum amount of data read(KB):  "${properties.maxInputMemory!}"</li>
  <li>Maximum amount of data generated(KB):  "${properties.maxOutputMemory!}"</li>
</ul>

<#include "tail.ftl">