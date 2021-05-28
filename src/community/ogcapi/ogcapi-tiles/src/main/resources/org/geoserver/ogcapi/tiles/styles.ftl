<#global pagetitle=model.tileLayerId>
<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item'><a href='"+serviceLink("collections")+"'>Collections</a></li><li class='breadcrumb-item'><a href='" + serviceLink("collections/" + model.tileLayerId) + "'>" + pagetitle+ "</a></li><li class='breadcrumb-item'>styles</li>">
<#include "common-header.ftl">
       <h2>Styles for ${model.tileLayerId}</h2>
       <p>This document lists the styles available for the ${model.tileLayerId} collection:</p>
       <ul>
       <hr/>
       <#list model.styles as style>
       <h3><#if style.style??>${style.id}<#else>Default style</#if></h3>
       
       <#if style.title??><p>${style.title}</p><#else>Default style</#if>
       <p><a href="${serviceLink("collections/${model.tileLayerId}/styles/${style.id}/map/tiles?f=html")}">Map tiles</a></p>
       <hr/>
       </#list>
       </ul>
<#include "common-footer.ftl">
