<#global pagetitle=model.id>
<#global pagepath="/collections/"+model.id>
<#global pagecrumbs="<a href='"+serviceLink("")+"'>Home</a><a href='"+serviceLink("collections")+"/collections'>Collections</a><b>"+model.id+"</b>">
<#include "common-header.ftl">
       <h2>GeoServer Styles</h2>
       <p>This document lists all the styles available in the Styles service.<br/>
       This document is also available as <#list model.getLinksExcept(null, "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.</p>
       
       <ul>
       <#list model.styles as style>
       <li><a href="${serviceLink("styles/${style.id}/metadata?f=html")}">${style.id}</a>: ${style.title!"N/A"}</li>
       </#list>
       </ul>
<#include "common-footer.ftl">
