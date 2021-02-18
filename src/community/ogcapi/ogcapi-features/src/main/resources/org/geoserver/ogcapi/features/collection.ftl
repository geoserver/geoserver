<#include "common-header.ftl">
  <div id="breadcrumb" class="py-2 mb-4">
    <nav aria-label="breadcrumb">
      <ol class="breadcrumb mb-0">
        <#--  <li class="breadcrumb-item"><a href="#">Home</a></li>
        <li class="breadcrumb-item"><a href="#">Library</a></li>
        <li class="breadcrumb-item active" aria-current="page">Data</li>  -->
      </ol>
    </nav>
  </div>
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
    </div>
    <div class="col-xs col-md-6 col-lg-4 text-center p-5">
      <#--  MAP PLACEHOLDER  -->
    </div>
  </div>
<#include "common-footer.ftl">
