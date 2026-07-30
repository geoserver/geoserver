<#global pagecrumbs>
  <li class='breadcrumb-item'><a href='${serviceLink("")}'>Home</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections")}'>Collections</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections/${model.published.prefixedName()}")}'>${model.published.prefixedName()}</a></li>
  <li class='breadcrumb-item active'>Styles</li>
</#global>
<#include "common-header.ftl">

  <h1>Styles for ${model.published.prefixedName()}</h1>
  <p class="my-4">This document lists the styles available to render the ${model.published.prefixedName()} collection as a map.</p>

  <div class="row">
    <#list model.styles as style>
    <div class="col-xs-12 col-md-6 col-lg-4 pb-4">
      <div class="card h-100">
        <div class="card-header">
          <h2><#if style.style??>${style.id}<#else>Default style</#if></h2>
        </div>
        <div class="card-body">
          <#if style.title??><p>${style.title}</p></#if>
          <#assign legendUrl = style.getLinkUrl('https://www.opengis.net/def/rel/ogc/1.0/legend', 'image/png')!''>
          <#if legendUrl?has_content><img src="${legendUrl}&legend-options=fontAntiAliasing:true" alt="Legend for ${style.id}"/></#if>
        </div>
        <#assign mapFormatsResource = style>
        <#assign mapFormatsRel = 'items'>
        <#assign mapFormatsLabel = 'Map as'>
        <#include "map-formats.ftl">
      </div>
    </div>
    </#list>
  </div>

  <script src="${resourceLink('webresources/ogcapi/maps-collections.js')}"></script>

<#include "common-footer.ftl">
