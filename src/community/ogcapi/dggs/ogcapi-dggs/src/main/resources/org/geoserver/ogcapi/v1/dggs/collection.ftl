<#include "common-header.ftl">

  <div class="card my-4">
    <div class="card-header">
      <h2>${model.id}</h2>
    </div>
    <#assign collection=model>
    <#include "collection_include.ftl">
  </div>

  <div class="card my-4">
    <div class="card-header">
      <h2>Feature schema</h2>
    </div>
    <div class="card-body">
    <#if model.schema??>
      <ul>
      <#list model.schema.attributes as attribute>
        <#assign idx=attribute.type?last_index_of(".") + 1>
        <li><b>${attribute.name}</b>: ${attribute.type?substring(idx)}</li>
      </#list>
      </ul>
    <#else>
      Attribute information is not available.
    </#if>
    </div>
  </div>
       
  <div class="card my-4">
    <div class="card-header"> 
      <h2>DGGS specific information</h2>
    </div>
    <div class="card-body">
      The zones follow the ${model.dggsId} DGGS.<br/>
      Available resolutions are:<br/>
      <ul>
      <#list model.resolutions as r>
        <li>${r}</li>
      </#list>
      </ul>
    </div>
  </div>
  
<#include "common-footer.ftl">
