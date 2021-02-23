<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item active'>GeoServer "+model.apiName+" Conformance</li>">
<#include "common-header.ftl">
  <h1 id="title">GeoServer ${model.apiName} Conformance</h1>
  <p class="my-4">
    This document lists the OGC API conformance classes that are implemented by this service.
  </p>

  <h2>Conformance classes:</h2>
  <ul>
  <#list model.conformsTo as conformsTo>
    <li><a href="${conformsTo}" target="_blank">${conformsTo}</a></li>
  </#list>
  </ul>
<#include "common-footer.ftl">
