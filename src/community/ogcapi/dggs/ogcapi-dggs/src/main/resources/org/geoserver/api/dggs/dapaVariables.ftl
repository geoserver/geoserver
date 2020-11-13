<#include "common-header.ftl">
       <h2>${model.collectionId} DAPA variables</h2>

       <ul>
       <#list model.variables as v>
         <li>${v.id}: ${v.description}</li>         
       </#list>
       </ul>
       
<#include "common-footer.ftl">
