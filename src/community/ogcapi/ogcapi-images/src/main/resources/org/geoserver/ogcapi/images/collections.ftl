<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item active'>Collections</li>">
<#include "common-header.ftl">
       <h2>GeoServer Image Mosaics Collections</h2>
       <p>This document lists all the image collections available in the Images service.</p>
       
       <#list model.collections as collection>
       <h2><a id="html_${collection.htmlId}_link" href="${serviceLink("collections/${collection.encodedId}", "text/html")}">${collection.id}</a></h2>
       <ul>
         <#if collection.title??> 
         <li><b>Title</b>: <span id="${collection.htmlId}_title">${collection.title}</span><br/></li>
         </#if>
         <#if collection.description??>
         <li><b>Description</b>: <span id="${collection.htmlId}_description">${collection.description!}</span><br/></li>
         </#if>
         <#assign spatial = collection.extent.spatial>
         <li><b>Geographic extents</b>:
         <ul>
         <#list spatial as se>
         <li>${se.getMinX()}, ${se.getMinY()}, ${se.getMaxX()}, ${se.getMaxY()}.</li>
         </#list>
         </ul>
         </li>
         </ul>
       </#list>
<#include "common-footer.ftl">
