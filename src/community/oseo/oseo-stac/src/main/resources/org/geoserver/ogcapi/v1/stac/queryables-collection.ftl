<#global pagecrumbs>
  <li class='breadcrumb-item'><a href='${serviceLink("")}'>Home</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections")}'>Collections</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections/" + model.collectionId)}'>${model.collectionId}</a></li>
  <li class='breadcrumb-item active'>Queryables</a></li>
</#global>

<#include "common-header.ftl">
  <div class="card my-4">
    <div class="card-header">
      <h2>Queryables for ${model.collectionId}</h2>
    </div>
    <#include "queryables-common.ftl">
  </div>
    
<#include "common-footer.ftl">
