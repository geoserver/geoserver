    <#-- 
    Body section of the GetFeature template, it's provided with one feature collection, and
    will be called multiple times if there are various feature collections
    -->
    <h2><a href="${serviceLink("/collections/${collection}")}">${data.type.name}</a></h2>
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
          
          <#if zoneLink(featureInfo, feature)??>
          <td><a href="${zoneLink(featureInfo, feature)}">${feature.fid}</a></td>
          <#else>
          <td>${feature.fid}</td>
          </#if>    
          <#list feature.attributes as attribute>
            <#if !attribute.isGeometry>
              <td>${attribute.value?string}</td>
            </#if>
          </#list>
          </tr>
        </#list>
        </table>
    </div>
