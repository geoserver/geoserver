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
        <#include "collection_include.ftl">
      </div>
    </div>
  </div>

  <script src="${resourceLink('webresources/ogcapi/maps-collections.js')}"></script>

<#include "common-footer.ftl">
