<#include "common-header.ftl">
       <h2>${model.id}</h2>
       <#assign collection=model>
       <#include "collection_include.ftl">

       <h2>Feature schema</h2>
       <#if model.schema??>
       <ul>
       <#list model.schema.attributes as attribute>
         <#assign idx=attribute.type?last_index_of(".") + 1>
         <li><b>${attribute.name}</b>: ${attribute.type?substring(idx)}</li>
       </#list>
       </ul>
       <#else>
       Attribute information is not available.
       </#if>
       
       <h2>DGGS specific information</h2>
       The zones follow the ${model.dggsId} DGGS.<br/>
       Available resolutions are: <#list model.resolutions as r>${r} </#list>
<#include "common-footer.ftl">
