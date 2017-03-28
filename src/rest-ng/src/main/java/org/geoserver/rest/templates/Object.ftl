<html>
  <body>
  <#if properties??>
  This is a generic template for ${className}. 
  <br>
  Properties:
  <ul>
      <#list properties.keySet() as p>
        <li>${p}: ${properties[p]}
      </#list>
  </ul>
  <#else>
      <table border="1">
      <#list values as v>
        <tr>
         <#list v.properties.values() as p>
           <td>${p}</td>
         </#list>
        </tr>
      </#list>
      </table>
  </#if>
  
  </body>
 </html>