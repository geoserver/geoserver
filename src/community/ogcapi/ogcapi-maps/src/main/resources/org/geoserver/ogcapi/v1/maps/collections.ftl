<#include "common-header.ftl">
       <h2>GeoServer Maps Collections</h2>
       <p>This document lists all the collections available in the Maps service.</p>
       
       <#list model.collections as collection>
       <h2><a href="${serviceLink("collections/${collection.id}")}">${collection.id}</a></h2>
       <#include "collection_include.ftl">
       </#list>
<#include "common-footer.ftl">
