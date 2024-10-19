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
      <#if collection.extent.temporal??>
            <#assign temporal = collection.extent.temporal>
            <li id="${collection.htmlId}_temporal"><b>Temporal extent</b>: ${temporal.minValue}/${temporal.maxValue}</li>
      </#if>
      <li> <a id="html_${collection.htmlId}_link" href="${collection.getLinkUrl('styles', 'text/html')!}">Map styles</a>.
                 
      ${htmlExtensions(collection)?no_esc}
      </ul>
      
