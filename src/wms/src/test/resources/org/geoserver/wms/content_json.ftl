<#list features as feature>
{
"content" : "this is the content",
"type": "${type.name}"
<#list feature.attributes as attribute>
,"${attribute.name}": "${attribute.value}"
</#list>
}
</#list>