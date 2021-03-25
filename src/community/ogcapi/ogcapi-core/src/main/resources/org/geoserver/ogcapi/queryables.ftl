<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item active'><a href='"+serviceLink("collections")+"'>Collections</a></li><li class='breadcrumb-item active'>"+model.collectionId+"</li>">
<#include "common-header.ftl">

  <div class="card my-4">
    <div class="card-header">
      <h2>Queryables for ${model.collectionId}</h2>
    </div>
    <div class="card-body">
      <#if model.queryables??>
      <ul id="queryables">
      <#list model.queryables as queryable>
        <li><b>${queryable.id}</b>: ${queryable.type}</li>
      </#list>
      </ul>
      <#else>
      <div class="p-3 mb-2 bg-warning text-dark">No queryables found.</div>
      </#if>
    </div>
  </div>
    
<#include "common-footer.ftl">
