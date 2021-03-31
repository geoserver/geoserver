<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item active'>Filter capabilities</li>">
<#include "common-header.ftl">
       <h2>Functions</h2>
       <#list model.functions as f>
         <h3>${f.name}</h3>
         <ul>
         <li>Returns: <#list f.returns.type as t>${t} </#list></li>
         <li>Arguments:
         <table class="function-table">
         <tr><th>Name</th><th>Description</th><th>Type</th></tr>
         <#list f.arguments as arg>
            <tr><td>${arg.title}</td><td>${arg.description!""}</td><td><#list f.returns.type as t>${t} </#list></td></tr>
         </#list>
         </table>
         </li>
         </ul>
       </#list>
<#include "common-footer.ftl">
