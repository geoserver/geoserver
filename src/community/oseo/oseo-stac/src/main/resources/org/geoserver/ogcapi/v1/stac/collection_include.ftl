<#setting datetime_format="iso">
<div class="card-body">
  <ul>
    <#assign a = collection.attributes />
    <#if a.title.rawValue??> 
      <li data-tid='title'><b>Title</b>: <span id="${collection.fid}_title">${a.title.value}</span><br/></li>
    </#if>
    <#if a.description.rawValue??>
      <li data-tid='description'><b>Description</b>: <span id="${collection.fid}_description">${a.description.value}</span><br/></li>
    </#if>
    <li data-tid='license'><b>License</b>: ${a.license.value!"proprietary"}</li>
    <#assign bounds = collection.bounds>
    <li><b>Extents</b>:
      <ul>
        <li data-tid='gbounds'>Geographic (WGS84): ${bounds.getMinX()}, ${bounds.getMinY()}, ${bounds.getMaxX()}, ${bounds.getMaxY()}.</li>
        <li data-tid='tbounds'>Temporal: ${(a.timeStart.rawValue)!"unbounded"}/${(a.timeEnd.rawValue)!"unbounded"}</li>
      </ul>
    </li>
  </ul>
</div>
      
