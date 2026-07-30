<div class="card-body">
  <ul>
    <#if collection.title??>
      <li><b>Title</b>: <span id="${collection.htmlId}_title">${collection.title}</span><br/></li>
      </#if>
      <#if collection.description??>
      <li><b>Description</b>: <span id="${collection.htmlId}_description">${collection.description!}</span><br/></li>
      </#if>
      <#-- only the single collection page sets this: in the list the CRS of every collection is just noise -->
      <#if (showStorageCrs!false) && collection.storageCrs??>
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
      <#if collection.extent.temporal??>
      <#assign temporal = collection.extent.temporal>
      <li><span id="${collection.htmlId}_temporal"><b>Temporal extent</b>: ${temporal.minValue}/${temporal.maxValue}</span></li>
      </#if>
      <#if collection.getLinkUrl('styles', 'text/html')??>
      <li>Available <a id="html_${collection.htmlId}_styles" href="${collection.getLinkUrl('styles', 'text/html')}">styles</a> to render this collection as a map.</li>
      </#if>
  </ul>
</div>
<#assign mapFormatsResource = collection>
<#assign mapFormatsRel = 'defaultMap'>
<#assign mapFormatsLabel = 'Default map as'>
<#assign mapFormatsLinkId = 'html_${collection.htmlId}_link'>
<#include "map-formats.ftl">
