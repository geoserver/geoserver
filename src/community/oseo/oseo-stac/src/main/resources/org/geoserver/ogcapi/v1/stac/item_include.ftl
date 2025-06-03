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
        <#if a.cloudCover.value?has_content><li data-tid='ccover'>Cloud cover: ${a.cloudCover.value}</#if> 
      </li>
    </li>
  </ul>
</div>
      
