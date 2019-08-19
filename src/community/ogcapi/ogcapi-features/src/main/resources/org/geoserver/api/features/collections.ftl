<#include "common-header.ftl">
       <h2>GeoServer Feature Collections</h2>
       <p>This document lists all the collections available in the Features service.<br/>
       This document is also available as <#list model.getLinksExcept(null, "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.</p>
       
       <#list model.collections as collection>
       <h2><a href="${serviceLink("ogc/features/collections/${collection.id}")}">${collection.id}</a></h2>
       <#include "collection_include.ftl">
       </#list>
<#include "common-footer.ftl">
