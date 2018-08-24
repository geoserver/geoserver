<html>
  <body>
  This is a generic template for ${className}. 
  <br>
  Properties:
  <ul>
  <#list properties.keySet() as p>
    <li>${p}: ${properties[p]}
  </#list>
  </ul>
  </body>
</html>