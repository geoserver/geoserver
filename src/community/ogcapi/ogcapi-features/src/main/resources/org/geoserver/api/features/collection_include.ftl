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
      <li>Data as <a id="html_${collection.htmlId}_link" href="${collection.getLinkUrl('items', 'text/html')!}&limit=${service.maxNumberOfFeaturesForPreview}">HTML</a>.           
      Collection items are also available in the following formats:
      <select onchange="window.open(this.options[this.selectedIndex].value + '&limit=${service.maxNumberOfFeaturesForPreview}');this.selectedIndex=0" >
      <option value="none" selected>--Please choose an option--</option>
      <#list collection.getLinksExcept("items", "text/html") as link>
      <option value="${link.href}">${link.type}</option>
      </#list>
      </select>
      <#if collection.mapPreviewURL??>
      <li>The layer can also be explored in this <a href="${collection.mapPreviewURL}">map preview</a></li>
      </#if>
      </ul>