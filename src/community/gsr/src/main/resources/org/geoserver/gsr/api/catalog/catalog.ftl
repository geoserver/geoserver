<#include "common-header.ftl">
   <#include "breadcrumbs.ftl">
   <h2>Folder: ${model.name}</h2>
   <b>Current version: ${model.currentVersion}</b><br>
   <!-- View Footprints In: TODO?-->
   <br/>
   <p><b>Folders: </b></p>
   <ul>
   <#list model.folders as folder>
   <li><a href="${serviceLink("${folder}")}">${folder}</a></li>
   </#list>
   </ul>
   <p><b>Services: </b></p>
   <ul>
   <#list model.services as service>
   <li><a href="${serviceLink("${service.name}/${service.type}")}">${service.name}</a> (${service.type})</li>
   </#list>
   </ul>
   <#include "interfaces.ftl">
<#include "common-footer.ftl">