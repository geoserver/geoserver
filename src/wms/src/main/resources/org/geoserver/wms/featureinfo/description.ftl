<h4>${typeName}</h4>

<#if attributes?exists>
<ul class="textattributes">
<#list attributes as a>
  <#if ! a.isGeometry && a.value?? && a.value != "" && a.value != " "><li><strong><span class="atr-name">${a.name}</span>:</strong> <span class="atr-value">${a.value}</span></li></#if>
</#list>
</ul>
</#if>
