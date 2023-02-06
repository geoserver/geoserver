  <h1>${model.id} ${model.dataType} tiles <#if model.styleId??> - ${model.styleId}</#if></h1>

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
