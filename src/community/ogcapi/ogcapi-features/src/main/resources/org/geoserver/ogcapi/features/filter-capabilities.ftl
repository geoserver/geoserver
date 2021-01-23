<#include "common-header.ftl">
       <h2>Filter capabilities</h2>
       <#list model.capabilities as capability>
         <h3>${capability.name}</h3>
         <ul>
           <#list capability.operators as op>
             <li>${op}</li>
           </#list>
         </ul>
       </#list>
       <h2>Functions</h2>
       <#list model.functions as f>
         <h3>${f.name}</h3>
         <p>Returns: ${f.returns.type}</p>
         <p>Arguments:
         <ul>
         <#list f.arguments as arg>
            <li>${arg.name}: ${arg.type}
         </#list>
         </ul>
         </p>
       </#list>
<#include "common-footer.ftl">
