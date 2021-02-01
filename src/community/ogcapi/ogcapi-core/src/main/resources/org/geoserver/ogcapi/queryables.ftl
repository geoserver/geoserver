<#include "common-header.ftl">
       <h2>Queryables for ${model.collectionId}</h2>
       <#if model.queryables??>
       <ul id="queryables">
       <#list model.queryables as queryable>
         <li><b>${queryable.id}</b>: ${queryable.type}</li>
       </#list>
       </ul>
       <#else>
       No queryables found.
       </#if>
<#include "common-footer.ftl">
