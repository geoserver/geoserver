<#include "head.ftl">

<#if properties.workspaceName != 'NO_WORKSPACE'>
    Workspace Name:  "${properties.workspaceName}"
</#if>


<h4>Service Metadata</h4>

<ul>
  <li>WCS Enabled:  "${properties.enabled}"</li>
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

<h4>Coverage processing</h4>

<ul>
  <li>Use subsampling:  "${properties.isSubsamplingEnabled}"</li>
  <li>Overview policy:  "${properties.overviewPolicy}"</li>
</ul>

<h4>Resource Consumption Limits</h4>

<ul>
  <li>Maximum amount of data read(KB):  "${properties.maxInputMemory}"</li>
  <li>Maximum amount of data generated(KB):  "${properties.maxOutputMemory}"</li>
</ul>

<#include "tail.ftl">