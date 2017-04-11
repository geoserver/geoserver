<#include "head.ftl">

<#if properties.workspaceName??>
    Workspace Name:  "${properties.workspaceName}"
</#if>


<h4>Service Metadata</h4>

<ul>
  <li>WFS Enabled:  "${properties.enabled!}"</li>
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

<h4>Features</h4>

<ul>
  <li>Maximum number of features:  "${properties.maxFeatures!}"</li>
  <li>Return bounding box with every feature:  "${properties.isFeatureBounding!}"</li>
  <li>Ignore maximum number of features when calculating hits: ${properties.hitsIgnoreMaxFeatures!}"</li>
  <li>Maximum number of features for preview (Values &lt;= 0 use the maximum number of features): ${properties.maxNumberOfFeaturesForPreview!}"</li>
  <li>Service Level:  "${properties.serviceLevel!}"</li>
</ul>

<h4>Conformance and Encoding</h4>

<ul>
  <li>Encode canonical WFS schema location:  "${properties.isCanonicalSchemaLocation!}"</li>
  <li>
    <#if properties.encodeFeatureMember = 'true'>
      One "featureMembers" element
    <#else>
      Multiple "featureMembers" element
    </#if>
  </li>
</ul>

<#include "tail.ftl">