<#include "common-header.ftl">
       <h2>${model.name}</h2>
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
<#include "common-footer.ftl">
