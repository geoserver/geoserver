<#include "common-header.ftl">
       <h2>GeoServer DGGS collections</h2>
       <p>This document lists all the collections available in the DGGS service.<br/>
       
       <#list model.collections as collection>
       <h2><a href="${serviceLink("collections/${collection.id}")}">${collection.id}</a></h2>
       <#include "collection_include.ftl">
       </#list>
<#include "common-footer.ftl">
