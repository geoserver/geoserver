<#include "common-header.ftl">
  <#assign collection=model.collection>
  <h1 id="title">${collection.name.value}</h1>

  <h1>This is a collection with layers</h1>
  
  <#list collection.layers as layerWrapper>
    <#assign layer=layerWrapper.rawValue>
    <h2>${layer.workspace.value}:${layer.layer.value}</h2>
    <p class="title">${layer.title.value}</p>
    <p class="description">${layer.description.value}</p>
    
    <#if layer.styles??>
      <h3>Styles</h3>
        <#list layer.styles as styleWrapper>
          <#assign style=styleWrapper.rawValue>
          <p class="style">${(style.name.value)!}: ${(style.title.value)!}</p>
        </#list>
    </#if>
    
    <#assign wms=layer.services.rawValue.wms.rawValue>
    <#if wms??>
        <h3>WMS</h3>
        <p>Enabled: ${wms.enabled.value}</p>
        <p>Formats: 
        <ul>
        <#list wms.formats as format>
          <li>${format.value}</li>
        </#list>
        </ul>
        <p><a class="wmsCapabilities" href="${genericServiceLink('wms', 'service', 'WMS', 'request', 'GetCapabilities')}">Capabilities URL</a></p>
    </#if>
    
    <#assign wcs=layer.services.rawValue.wcs.rawValue>
    <#if wcs??>
        <h3>WCS</h3>
        <p>Enabled: ${wcs.enabled.value}</p>
        <p>Formats: 
        <ul>
        <#list wcs.formats as format>
          <li>${format.value}</li>
        </#list>
        </ul>
    </#if>
    
    <#assign wmts=layer.services.rawValue.wmts.rawValue>
    <#if wmts??>
        <h3>WMTS</h3>
        <p>Enabled: ${wmts.enabled.value}</p>
        <p>Formats: 
        <ul>
        <#list wmts.formats as format>
          <li>${format.value}</li>
        </#list>
        </ul>
    </#if>
  </#list>

<#include "common-footer.ftl">
