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
    <li><b>Properties</b>:
      <ul>
        <#assign ca=a.collection.rawValue>
        <#if ca.instrument.value??><li data-tid='instrument'>Instrument: ${ca.instrument.value}</#if>
        <#if a.cloudCover.value??><li data-tid='ccover'>Cloud cover: ${a.cloudCover.value}</#if> 
        <#if a.illuminationAzimuthAngle.value?has_content><li>Sun azimuth: ${a.illuminationAzimuthAngle.value}</#if>
        <#if a.illuminationElevationAngle.rawValue?has_content><li>Sun elevation: ${a.illuminationElevationAngle.value}</#if>
      </li>
    </li>
  </ul>
</div>
      
