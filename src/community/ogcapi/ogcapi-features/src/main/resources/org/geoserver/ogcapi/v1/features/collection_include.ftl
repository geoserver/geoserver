<div class="card-body">
  <ul>
    <#if collection.title??>
      <li><b>Title</b>: <span id="${collection.htmlId}_title">${collection.title}</span><br/></li>
      </#if>
      <#if collection.description??>
      <li><b>Description</b>: <span id="${collection.htmlId}_description">${collection.description!}</span><br/></li>
      </#if>
      <#if collection.storageCrs??>
      <li><b>Storage CRS</b>: <span id="${collection.htmlId}_storageCrs">${collection.storageCrs!}</span><br/></li>
      </#if>
      <#assign spatial = collection.extent.spatial>
      <li><b>Geographic extents</b>:
      <ul>
      <#list spatial as se>
      <li>${se.getMinX()}, ${se.getMinY()}, ${se.getMaxX()}, ${se.getMaxY()}.</li>
      </#list>
      </ul>
      </li>

      <li>Queryables as <a id="html_${collection.htmlId}_queryables" href="${collection.getLinkUrl('queryables', 'text/html')!}">HTML</a>.
      </li>
      <#if collection.mapPreviewURL??>
      <li>The layer can also be explored in this <a href="${collection.mapPreviewURL}">map preview</a></li>
      </#if>
      <#-- TODO when upgrading Freemaker add ?no_esc to avoid html escaping -->
      ${htmlExtensions(collection)}
  </ul>
</div>
<div class="card-footer">
  <div class="row">
    <div class="col-auto pe-0 py-1">
      Data as <a id="html_${collection.htmlId}_link" class="btn btn-outline-primary btn-sm" href="${collection.getLinkUrl('items', 'text/html')!}&limit=${service.maxNumberOfFeaturesForPreview}">HTML</a>
      or choose another format:
    </div>
    <div class="col-auto py-1">
      <select class="form-select form-select-sm" onchange="window.open(this.options[this.selectedIndex].value + '&limit=${service.maxNumberOfFeaturesForPreview}');this.selectedIndex=0" >
        <option value="none" selected>-- Please choose a format --</option>
        <#list collection.getLinksExcept("items", "text/html") as link>
        <option value="${link.href}">${link.type}</option>
        </#list>
      </select>
    </div>
  </div>
</div>

