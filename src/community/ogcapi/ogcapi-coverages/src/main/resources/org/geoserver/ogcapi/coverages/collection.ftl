<#global pagetitle=model.id>
<#global pagepath="/collections/"+model.id>
<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item active'><a href='"+serviceLink("collections")+"'>Collections</a></li><li class='breadcrumb-item active'>"+model.id+"</li>">
<#include "common-header.ftl">

  <div class="row">
    <div class="col-xs col-md-6 col-lg-8">
      <div class="card my-4">
        <div class="card-header">
          <h2>${model.id}</h2>
        </div>
        <#assign collection=model>
        <#include "collection_include.ftl">
      </div>

      <div class="card my-4">
        <div class="card-header">
          <h2>More information</h2>
        </div>
        <div class="card-body">
          <a href="${collection.getLinkUrl('domainset', 'application/json')!}">Domain set as JSON</a>
        </div>
      </div>
    </div>
    
  </div>
<#include "common-footer.ftl">
