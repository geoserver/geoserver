<#global pagetitle=service.title!"GeoServer Tiles 1.0 Service">
<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item active'>"+pagetitle+"</li>">
<#include "common-header.ftl">

  <h1>${pagetitle}</h1>
  <p class="my-4">
    ${service.abstract!""}<br/>
    This is the landing page of the Tiles 1.0 service, providing links to the service API and its contents.<br/> 
    This document is also available as
    <#list model.getLinksExcept("landingPage", "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.
  </p>
       
  <div class="row my-3">
    <div class="col-6 col-xl-3 mb-3">
      <div class="card h-100">
        <div class="card-body">
          <h2>API definition</h2>
          <p>The <a id="htmlApiLink" href="${model.getLinkUrl('api', 'text/html')!}"> API document</a> provides a machine processable description of this service API conformant to OpenAPI 3. 
          <br/> 
          This API document is also available as
          <#list model.getLinksExcept("api", "application/json") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.
          </p>
        </div>
      </div>
    </div>
    
    <div class="col-6 col-xl-3 mb-3">
      <div class="card h-100">
        <div class="card-body">
          <h2>Tile matrix sets</h2>
          <p>Tiles are cached on <a id="tileMatrixSetsLink" href="${model.getLinkUrl('tileMatrixSets', 'text/html')!}">tile matrix sets</a>, defining tile layouts and zoom levels.
          <br/> 
          This page is also available as
          <#list model.getLinksExcept("tileMatrixSets", "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.
          </p> 
        </div>
      </div>
    </div>

    <div class="col-6 col-xl-3 mb-3">
      <div class="card h-100">
        <div class="card-body">
          <h2>Tiled Collections</h2>
          <p>The <a id="htmlCollectionsLink" href="${model.getLinkUrl('collections', 'text/html')!}"> collections page</a> provides a list of all the tiled collections available in this service. 
          <br/> 
          This tiled collections page is also available as
          <#list model.getLinksExcept("collections", "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.
          </p>
        </div>
      </div>
    </div>

    <div class="col-6 col-xl-3 mb-3">
      <div class="card h-100">
        <div class="card-body">
          <#include "landingpage-conformance.ftl">
        </div>
      </div>
    </div>
    
  </div>

  <h2>Contact information</h2>
  <address>
    <ul>
      <li>Server managed by ${contact.contactPerson!"-unspecified-"}</li>
      <li>Organization: ${contact.contactOrganization!"-unspecified-"}</li>
      <li>Mail: <a href="mailto:${contact.contactEmail!''}">${contact.contactEmail!"-unspecified-"}</a></li>
    </ul>
  </address>
<#include "common-footer.ftl">