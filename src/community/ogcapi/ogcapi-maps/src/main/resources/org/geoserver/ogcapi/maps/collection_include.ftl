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
      <li> <a id="html_${collection.htmlId}_link" href="${collection.getLinkUrl('styles', 'text/html')!}">Map styles</a>.
                 
      <#-- TODO when upgrading Freemaker add ?no_esc to avoid html escaping --> 
      ${htmlExtensions(collection)}
      </ul>
      
