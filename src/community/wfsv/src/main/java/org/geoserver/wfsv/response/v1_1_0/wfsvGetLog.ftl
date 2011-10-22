<#-- 
Very simple HTML table output for logs... we should 
add a title with the feature type name and the starting/ending
version, but that would mean changing the return type of the GetLog call... sigh...
-->
<html>
<body>
<table border='1' cellspacing="0" cellpadding="3">

<tr>
  <th>Revision</th>
  <th>Author</th>
  <th>Date</th>
  <th>Message</th>
</tr>

<#list features as feature>
<tr>
  <td align="right">
    ${feature.attributes[0].value}
  </td>
  <td>
    ${feature.attributes[1].value}
  </td>
  <td align="right">
    ${feature.attributes[2].value}
  </td>
  <td>
    ${feature.attributes[3].value}
  </td>
</tr>
</#list>

</table>
</body>
</html>