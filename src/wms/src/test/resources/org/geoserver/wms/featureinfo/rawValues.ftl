<table border='1'>

<tr>
  <th colspan='${attributes?size}' scope='col'>${typeName}</th>
</tr>

<tr>
<#list attributes as a>
  <td>${a.name}</td>
</#list>
</tr>

<tr>
<#list attributes as a>
  <td>
  <#if a.isGeometry >
[GEOMETRY]
  <#else>
${a.rawValue.toString()}
  </#if>
  </td>
</#list>
</tr>

</table>
