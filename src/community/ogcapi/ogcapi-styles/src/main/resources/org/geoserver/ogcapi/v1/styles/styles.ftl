<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item active'>GeoServer Styles</li>">
<#include "common-header.ftl">
  <h1>GeoServer Styles</h1>
  <p class="my-4">
    This document lists all the styles available in the Styles service.
  </p>
  
  <ul>
  <#list model.styles as style>
    <li><a href="${serviceLink("styles/${style.id}/metadata?f=html")}">${style.id}</a>: ${style.title!"N/A"}</li>
  </#list>
  </ul>
<#include "common-footer.ftl">
