<#global pagetitle=model.id>
<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item'><a href='"+serviceLink("collections")+"'>Collections</a></li><li class='breadcrumb-item'><a href='" + serviceLink("collections/" + model.id) + "'>" + pagetitle+ "</a></li><li class='breadcrumb-item'>tiles</li>">
<#include "common-header.ftl">
  <h1>${model.id}</h1>

  <div class="card my-4">
    <div class="card-header">
      <h2>Available tilesets:</h2>
    </div>
    <ul>
    <#list model.tilesets as tms>
      <li><a href="${tms.getLinkUrl('self', 'text/html')}">${tms.gridSubsetId}</a></li>
    </#list>
    </ul>
  </div>
  

<#include "common-footer.ftl">
