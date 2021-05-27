<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item active'>Collections</li>">
<#include "common-header.ftl">

  <h1 id="title">GeoServer STAC Collections</h1>
  <p class="my-4">
    This document lists all the collections available in the SpatioTemporal Asset Catalog.<br/>
    <#-- This document is also available as <#list model.getLinksExcept(null, "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>. -->
  </p>
  
  <div class="row">
    <#list model.collections.features as collection>
    <#assign a = collection.attributes />
    <div class="col-xs-12 col-md-6 col-lg-4 pb-4">
      <div class="card h-100  mb-3">
        <div class="card-header">
          <h2><a href="${serviceLink("collections/${a.name.value}")}">${a.name.value}</a></h2>
        </div>
        <#include "collection_include.ftl">
      </div>
    </div>
    </#list>
  </div>

<#include "common-footer.ftl">
