<ul>
<#list features as feature>
  <li><b>Type: ${type.name}</b> (id: <em>${feature.fid}</em>):
  <ul>
  <#list feature.attributes as attribute>
    <#if !attribute.isGeometry>
      <li>${attribute.name}: ${attribute.value}</li>
    </#if>
  </#list>
  </ul>
  </li>
</#list>
</ul>