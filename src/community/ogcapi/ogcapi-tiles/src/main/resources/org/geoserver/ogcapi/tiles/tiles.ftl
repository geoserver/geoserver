<#global pagetitle=model.id>
<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item'><a href='"+serviceLink("tileMatrixSets")+"'>Tile Matrix Sets</a></li><li class='breadcrumb-item active'>"+pagetitle+"</li>">
<#include "common-header.ftl">
  <h1>${model.id}</h1>

  <div class="card my-4">
    <div class="card-header">
      <h2>Formats available and url templates to access tiles</h2>
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
      <h2>Tile matrix sets in which the above URLs are available, and grid limits:</h2>
    </div>
    <#list model.tileMatrixSetLinks as tms>
    <div class="card-body">
      <div class="table-responsive-xs">
        <table class="table table-striped table-hover table-bordered">
          <thead>
            <tr class="table-info">
              <th colspan="5"><a href="${tms.tileMatrixSetURI}"$>${tms.tileMatrixSet}</a></th>
            </tr>
            <tr>
              <th>tileMatrixSetId</th>
              <th>minRow</th>
              <th>maxRow</th>
              <th>minCol</th>
              <th>maxCol</th>
            </tr>
          </thead>
          <tbody>
          <#list tms.tileMatrixSetLimits as limit>
            <tr>
              <td>${limit.tileMatrix}</td>
              <td>${limit.minTileRow}</td>
              <td>${limit.maxTileRow}</td>
              <td>${limit.maxTileCol}</td>
              <td>${limit.maxTileCol}</td>
            </tr>
          </#list>
          </tbody>
        </table>
      </div>
    </div>
    </#list>
  </div>
  

<#include "common-footer.ftl">
