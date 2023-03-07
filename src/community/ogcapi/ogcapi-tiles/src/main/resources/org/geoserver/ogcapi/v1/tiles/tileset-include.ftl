  <h1>${model.id} ${model.gridSubsetId} tiles <#if model.styleId??> - ${model.styleId}</#if></h1>
  
  This document describes tile access to tiles in the <a href="${model.tileMatrixSetDefinition}">${model.gridSubsetId}</a> gridset

  <div class="card my-4">
    <div class="card-header">
      <h2>Formats available and URL templates to access tiles</h2>
    </div>
    <div class="card-body">
      <ul>
      <#list model.links as link>
        <#if link.rel == 'item'>
        <li>${link.type}: <code>${link.href}</code></li> 
        </#if>
      </#list>
      </ul>
    </div>
  </div>

  <div class="card my-4">
    <div class="card-header">
      <h2>Tiles metadata available as</h2>
    </div>
    <div class="card-body">
      <ul>
      <#list model.links as link>
        <#if link.rel == 'describedBy'>
        <li>${link.type}: <code>${link.href}</code></li> 
        </#if>
      </#list>
      </ul>
    </div>
  </div>

  <div class="card my-4">
    <div class="card-header">
      <h2>Tile set limits</h2>
    </div>
    <ul>
    <div class="card-body">
      <div class="table-responsive-xs">
        <table class="table table-striped table-hover table-bordered">
          <thead>
            <tr>
              <th>tileMatrixSetId</th>
              <th>minRow</th>
              <th>maxRow</th>
              <th>minCol</th>
              <th>maxCol</th>
            </tr>
          </thead>
          <tbody>
          <#list model.tileMatrixSetLimits as limit>
            <tr>
              <td>${limit.tileMatrix}</td>
              <td>${limit.minTileRow}</td>
              <td>${limit.maxTileRow}</td>
              <td>${limit.minTileCol}</td>
              <td>${limit.maxTileCol}</td>
            </tr>
          </#list>
          </tbody>
        </table>
      </div>
    </div>
  </div>
