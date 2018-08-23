<#include "head.ftl">

<h4>WMS Quality of Service Metadata</h4>
<ul>
  <li>WMS Enabled:  "${activated.toString()}"</li>
</ul>

<#if wmsQosMetadata.operatingInfo?has_content>
    <h4>Operating Info</h4>
    <ul>
        <#list wmsQosMetadata.operatingInfo as a>
            <li>Status:  "${a.operationalStatus.href}"</li>
            <li>Title:  "${a.operationalStatus.title}"</li>
        	<li>Days of Week:  
        		<#list a.byDaysOfWeek as day>
        			<li>Days: "
        				<#list day.days as dw>
        					${dw}
        				</#list>"
        			</li>
        			<li>Start Time: "${day.startTime}"</li>
        			<li>End Time: "${day.endTime}"</li>
        		</#list> 
        	</li>
        </#list>
    </ul>
</#if>

<#if wmsQosMetadata.statements?has_content>
    <h4>Statements</h4>
    <ul>
        <#list wmsQosMetadata.statements as a>
            <li>Metric:  "${a.metric.href}"</li>
            <li>Title:  "${a.metric.title}"</li>
        	<li>Unit of metric:  "${a.meassure.uom}"</li>
        	<li>Value:  "${a.meassure.value}"</li>
        </#list>
    </ul>
</#if>

<#if wmsQosMetadata.representativeOperations?has_content>
    <h4>Representative Operations</h4>
    <ul>
        <#list wmsQosMetadata.representativeOperations as a>
        	<#if a.getMapOperations?has_content>
        	<ul>
	        	<h5>GetMap Operations</h5>
	        	<#list a.getMapOperations as op>
		            <li>HTTP Method:  "${op.httpMethod}"</li>
		            <ul>
		            <#list op.requestOptions as ro>
		            	<li>Layers:
		            		<ul>
		            		<#list ro.layerNames as ly>
		            			<li>${ly}</li>
		            		</#list>
		            		</ul>
		            	</li>
		            	<li>CRS: "${ro.crs}"</li>
		            	<li>Min X: "${ro.areaConstraint.minX}"</li>
		            	<li>Min Y: "${ro.areaConstraint.minY}"</li>
		            	<li>Max X: "${ro.areaConstraint.maxX}"</li>
		            	<li>Max X: "${ro.areaConstraint.maxY}"</li>
		            	<li>Image Width min: "${ro.imageWidth.minimunValue!}"</li>
		            	<li>Image Width max: "${ro.imageWidth.maximunValue!}"</li>
		            	<li>Image Height min: "${ro.imageHeight.minimunValue!}"</li>
		            	<li>Image Height max: "${ro.imageHeight.maximunValue!}"</li>
		            	<li>Output Formats:
		            		<ul>
		            			<#list ro.outputFormat as of>
		            				<li>${of}</li>
		            			</#list>
		            		</ul>
		            	</li>
		            </#list>
		            </ul>
		        </#list>
	       	</ul>
	       	</#if>
	       	<#if a.getFeatureInfoOperations?has_content>
	       	<ul>
	        	<h5>GetFeatureInfo Operations</h5>
	        	<#list a.getFeatureInfoOperations as op>
		            <li>HTTP Method:  "${op.httpMethod}"</li>
		            <ul>
		            <#list op.requestOptions as ro>
		            	<li>Layers:
		            		<ul>
		            		<#list ro.layerNames as ly>
		            			<li>${ly}</li>
		            		</#list>
		            		</ul>
		            	</li>
		            	<li>CRS: "${ro.crs}"</li>
		            	<li>Min X: "${ro.areaConstraint.minX}"</li>
		            	<li>Min Y: "${ro.areaConstraint.minY}"</li>
		            	<li>Max X: "${ro.areaConstraint.maxX}"</li>
		            	<li>Max X: "${ro.areaConstraint.maxY}"</li>
		            	<li>Image Width min: "${ro.imageWidth.minimunValue!}"</li>
		            	<li>Image Width max: "${ro.imageWidth.maximunValue!}"</li>
		            	<li>Image Height min: "${ro.imageHeight.minimunValue!}"</li>
		            	<li>Image Height max: "${ro.imageHeight.maximunValue!}"</li>
		            	<li>Output Formats:
		            		<ul>
		            			<#list ro.outputFormat as of>
		            				<li>${of}</li>
		            			</#list>
		            		</ul>
		            	</li>
		            </#list>
		            </ul>
		        </#list>
	       	</ul>
	       	</#if>
        </#list>
    </ul>
</#if>

<#if wmsQosMetadata.operationAnomalyFeed?has_content>
    <h4>Anomaly Feed</h4>
    <ul>
        <#list wmsQosMetadata.operationAnomalyFeed as a>
            <li>Metric:  "${a.href!}"</li>
            <li>Title:  "${a.title!}"</li>
        	<li>Format:  "${a.format!}"</li>
        	<li>Value:  "${a.abstracts[0].value!}"</li>
        </#list>
    </ul>
</#if>

<#include "tail.ftl">