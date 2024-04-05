<#include "head.ftl">
<style>
table {
border-collapse: collapse;
width: 100%;
}

th, td {
border: 1px solid #dddddd;
padding: 8px;
text-align: left;
}

th {
background-color: #f2f2f2;
}
</style>
Url checks
<table>
<tr>
  <th>Name</th>
  <th>Description</th>
  <th>Configuration</th>
  <th>Enabled</th>
</tr>
<#list values as c>
<tr>
  <td>${c.properties.name}</td>
  <td>${c.properties.description}</td>
  <td>${c.properties.configuration}</td>
  <td>${c.properties.enabled}</td>
</tr>
</#list>
</table>
<#include "tail.ftl">
