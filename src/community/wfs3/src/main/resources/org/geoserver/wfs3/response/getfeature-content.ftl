    <#-- 
    Body section of the GetFeature template, it's provided with one feature collection, and
    will be called multiple times if there are various feature collections
    -->
    <#if collection??>
      <#-- Expended only in WFS3 -->
      <h2><a href="${serviceLink("wfs3/collections/${collection}")}"></a></h2>
    <#else>
      <h2>${data.type.name}</h2>
    </#if>
    <div>
        <table class="features">
          <tr>
          <th>fid</th>
        <#list data.type.attributes as attribute>
          <#if !attribute.isGeometry>
            <th>${attribute.name}</th>
          </#if>
        </#list>
          </tr>
        
        <#assign odd = false>
        <#list data.features as feature>
          <#if odd>
            <tr class="odd">
          <#else>
            <tr>
          </#if>
          <#assign odd = !odd>
        
          <td>${feature.fid}</td>    
          <#list feature.attributes as attribute>
            <#if !attribute.isGeometry>
              <td>${attribute.value?string}</td>
            </#if>
          </#list>
          </tr>
        </#list>
        </table>
    </div>
