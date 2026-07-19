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
          <#assign legendUrl = style.getLinkUrl('legend', 'image/png')!''>
          <#if legendUrl?has_content><img src="${legendUrl}&legend-options=fontAntiAliasing:true" alt="Legend for ${style.id}"/></#if>
        </div>
        <div class="card-footer">
          <div class="row">
            <div class="col-auto pe-0 py-1">
              Map as <a class="btn btn-outline-primary btn-sm" href="${style.getLinkUrl('items', 'text/html')!}">HTML</a>
              or choose another format:
            </div>
            <div class="col-auto py-1">
              <select class="form-select form-select-sm form-select-open-limit">
                <option value="none" selected>-- Please choose a format --</option>
                <#list style.getLinksExcept('items', 'text/html') as link>
                <option value="${link.href}">${link.type}</option>
                </#list>
              </select>
            </div>
          </div>
        </div>
      </div>
    </div>
    </#list>
  </div>

  <script src="${resourceLink('webresources/ogcapi/maps-collections.js')}"></script>

<#include "common-footer.ftl">
