    <#-- 
    Body section of the GetFeature template, it's provided with one feature collection, and
    will be called multiple times if there are various feature collections
    -->
  <div id="breadcrumb" class="py-2 mb-4">
    <nav aria-label="breadcrumb">
      <ol class="breadcrumb mb-0">
        <#--  <li class="breadcrumb-item"><a href="#">Home</a></li>
        <li class="breadcrumb-item"><a href="#">Library</a></li>
        <li class="breadcrumb-item active" aria-current="page">Data</li>  -->
      </ol>
    </nav>
  </div>

  <#if collection??>
    <#-- Expended only in OGC Features -->
    <h1><a href="${serviceLink("/collections/${collection}")}">${data.type.name}</a></h1>
  <#else>
    <h1>${data.type.name}</h1>
  </#if>

  <div class="row">
    <div class="col-xs col-lg-8">
      <div class="table-responsive">
        <table class="table table-striped table-hover table-bordered">
          <thead>
            <tr>
              <th>fid</th>
            <#list data.type.attributes as attribute>
              <#if !attribute.isGeometry>
                <th>${attribute.name}</th>
              </#if>
            </#list>
            </tr>
          </thead>
          <tbody>
            <#list data.features as feature>
            <tr>
              <td>${feature.fid}</td>    
              <#list feature.attributes as attribute>
                <#if !attribute.isGeometry>
                  <td>${attribute.value?string}</td>
                </#if>
              </#list>
              </tr>
            </#list>
          </tbody>
        </table>
      </div>
    </div>
    <div class="col-xs col-lg-4 text-center p-5">
      <#--  MAP PLACEHOLDER  -->
    </div>
  </div>