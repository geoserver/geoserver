<#include "common-header.ftl">
   <h2>Folder: /</h2>
   <p><b>Current version: ${model.currentVersion}</b><br>
   <b>Services: </b>
   <ul>
   <#list model.services as service>
   <li><a href="${serviceLink("gsr/services/${service.name}/${service.type}")}">${service.name}</a> (${service.type})</li>
   </#list>
<#include "common-footer.ftl">