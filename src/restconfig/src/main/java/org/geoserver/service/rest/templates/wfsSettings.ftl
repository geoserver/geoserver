<#include "head.ftl">

<#if properties.workspaceName != 'NO_WORKSPACE'>
    Workspace Name:  "${properties.workspaceName}"
</#if>


<h4>Service Metadata</h4>

<ul>
  <li>WFS Enabled:  "${properties.enabled}"</li>
  <li>Strict CITE compliance:  "${properties.citeCompliant}"</li>
  <li>Maintainer:  "${properties.maintainer}"</li>
  <li>Online Resource:  "${properties.onlineResource}"</li>
  <li>Title:  "${properties.title}"</li>
  <li>Abstract:  "${properties.abstract}"</li>
  <li>Fees:  "${properties.fees}"</li>
  <li>Access Constraints:  "${properties.accessConstraints}"</li>
  <li>Keywords:  "${properties.keywords}"</li>
  <li>Name:  "${properties.name}"</li>
  <li>Versions:  "${properties.versions}"</li>
  <li>Schema Base URL:  "${properties.schemaBaseURL}"</li>
  <li>Verbose Messages:  "${properties.verbose}"</li>
</ul>

<h4>Features</h4>

<ul>
  <li>Maximum number of features:  "${properties.maxFeatures}"</li>
  <li>Return bounding box with every feature:  "${properties.isFeatureBounding}"</li>
  <li>Ignore maximum number of features when calculating hits: ${properties.hitsIgnoreMaxFeatures}"</li>
  <li>Maximum number of features for preview (Values &lt= 0 use the maximum number of features): ${properties.maxNumberOfFeaturesForPreview}"</li>
  <li>Service Level:  "${properties.serviceLevel}"</li>
</ul>

<h4>Conformance and Encoding</h4>

<ul>
  <li>Encode canonical WFS schema location:  "${properties.isCanonicalSchemaLocation}"</li>
  <li>
    <#if properties.encodeFeatureMember = 'true'>
      One "featureMembers" element
    <#else>
      Multiple "featureMembers" element
    </#if>
  </li>
</ul>

<#include "tail.ftl">