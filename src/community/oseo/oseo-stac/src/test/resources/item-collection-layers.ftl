<#setting datetime_format="iso">
<div class="card-body">
  <ul>
    <#assign a = item.attributes />
    <#assign bounds = item.bounds>
    <li><b>Extents</b>:
      <ul>
        <li data-tid='gbounds'>Geographic (WGS84): ${bounds.getMinX()}, ${bounds.getMinY()}, ${bounds.getMaxX()}, ${bounds.getMaxY()}.</li>
        <li data-tid='tbounds'>Temporal: ${(a.timeStart.rawValue)!"unbounded"} / ${(a.timeEnd.rawValue)!"unbounded"}</li>
      </ul>
    </li>
    <li><b>Collection layers</b>:
      <ul>
        <#assign ca=a.collection.rawValue>
        <#list ca.layers as layerWrapper>
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
      </li>
    </li>
  </ul>
</div>
      