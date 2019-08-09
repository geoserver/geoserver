<#macro fieldDescription(field)>
<li>${field.name}: (Type: ${field.type}, Alias: ${field.alias}<#if field.length??>, Length: ${field.length}</#if>, Editable: ${field.editable?if_exists?string})</li>
</#macro>