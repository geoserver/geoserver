<#include "head.ftl">
<h2>Cluster configuration:</h2>
<!-- prints the model -->
<!--ul><#list .data_model?keys as key><li>${key}:${.data_model[key]}</li></#list></ul-->

<ul>
	<#list properties.keySet() as key>
			<li><b>${key}</b> : "${properties.get(key)!""}"</li>
	</#list>
	
</ul>

<#include "tail.ftl">