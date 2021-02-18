<#include "common-header.ftl">
  <div id="breadcrumb" class="py-2 mb-4">
    <nav aria-label="breadcrumb">
      <ol class="breadcrumb mb-0">
        <li class="breadcrumb-item"><a href="#">Home</a></li>
        <li class="breadcrumb-item"><a href="#">Library</a></li>
        <li class="breadcrumb-item active" aria-current="page">Data</li>
      </ol>
    </nav>
  </div>
  
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
