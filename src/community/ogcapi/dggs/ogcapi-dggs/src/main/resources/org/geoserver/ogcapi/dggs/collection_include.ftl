<div class="card-body">
  <ul>
  <#if collection.title??> 
    <li><b>Title</b>: <span id="${collection.htmlId}_title">${collection.title}</span><br/></li>
  </#if>
  <#if collection.description??>
    <li><b>Description</b>: <span id="${collection.htmlId}_description">${collection.description!}</span><br/></li>
  </#if>
  <#assign spatial = collection.extent.spatial>
    <li><b>Geographic extents</b>:
      <ul>
      <#list spatial as se>
        <li>${se.getMinX()}, ${se.getMinY()}, ${se.getMaxX()}, ${se.getMaxY()}.</li>
      </#list>
      </ul>
    </li>
    <li><a id="html_${collection.htmlId}_link" href="${collection.getLinkUrl('zones', 'text/html')!}&limit=${service.maxNumberOfZonesForPreview}">DGGS zone listing</a>.
    </li>
    <#if (collection.getLinkUrl('ogc-dapa-processes', 'text/html'))??>
    <li><a id="html_${collection.htmlId}_link" href="${collection.getLinkUrl('ogc-dapa-processes', 'text/html')!}">DAPA processes</a>.</li>
    <li><a id="html_${collection.htmlId}_link" href="${collection.getLinkUrl('ogc-dapa-variables', 'text/html')!}">DAPA variables</a>.</li>
    </#if>
    <#if collection.mapPreviewURL??>
    <li>The layer can also be explored in this <a href="${collection.mapPreviewURL}">map preview</a></li>
    </#if>
    <#-- TODO when upgrading Freemaker add ?no_esc to avoid html escaping --> 
    ${htmlExtensions(collection)}
  </ul>
</div>
      
