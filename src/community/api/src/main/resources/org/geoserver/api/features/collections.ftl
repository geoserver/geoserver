<#include "common-header.ftl">
       <h2>GeoServer WFS3 collections</h2>
       <p>This document lists all the collections available in the WFS 3 service.<br/>
       This document is also available as <#list model.getLinksExcept(null, "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.</p>
       
       <#list model.collections as collection>
       <h2><a href="${serviceLink("wfs3/collections/${collection.name}")}">${collection.name}</a></h2>
       <#include "collection_include.ftl">
       </#list>
<#include "common-footer.ftl">