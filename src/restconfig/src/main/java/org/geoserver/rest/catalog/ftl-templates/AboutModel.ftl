<#include "head.ftl">
<h2>About:</h2>
<#if properties.manifests?is_collection>
<ul><#list properties.manifests as m>
	<li><h3>Resource name: ${m.name}</h3>
  		<ul><#list m.properties as k>
  			<li><b>${k}</b> : ${m.valuez[k_index]!''}</li></#list>
		</ul>
	</li></#list>
</ul>
</#if>
<#include "tail.ftl">