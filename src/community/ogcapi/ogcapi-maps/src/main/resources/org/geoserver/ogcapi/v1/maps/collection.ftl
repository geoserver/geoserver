<#global pagetitle=model.id>
<#global pagepath="/collections/"+model.id>
<#global pagecrumbs>
  <li class='breadcrumb-item'><a href='${serviceLink("")}'>Home</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections")}'>Collections</a></li>
  <li class='breadcrumb-item active'>${model.id}</li>
</#global>
<#include "common-header.ftl">

  <div class="row">
    <div class="col-12">
      <div class="card my-4">
        <div class="card-header">
          <h2>${model.id}</h2>
        </div>
        <#assign collection=model>
        <#assign showStorageCrs=true>
        <#include "collection_include.ftl">
        <#if model.getLinkUrl('queryables', 'text/html')??>
        <div class="card-body border-top">
          <ul>
            <li>Queryable <a id="html_${model.htmlId}_queryables" href="${model.getLinkUrl('queryables', 'text/html')}">attributes</a> usable in the map filter.</li>
          </ul>
        </div>
        </#if>
      </div>
    </div>
  </div>

  <script src="${resourceLink('webresources/ogcapi/maps-collections.js')}"></script>

<#include "common-footer.ftl">
