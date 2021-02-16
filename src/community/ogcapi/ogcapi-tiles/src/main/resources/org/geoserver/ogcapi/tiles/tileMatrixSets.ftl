<#global pagetitle="Tile matrix sets">
<#global pagepath="/collections/"+pagetitle>
<#global pagecrumbs="<a href='"+serviceLink("")+"'>Home</a><a href='"+serviceLink("collections")+"'>Collections</a><b>"+pagetitle+"</b>">
<#include "common-header.ftl">
       <h1>${pagetitle}</h1>
       <p>This document lists all the available tile matrix sets, linking to their definitions:<br/>
       <ul>
       <#list model.tileMatrixSets as tileMatrixSet>
           <li><a href="${serviceLink("/tileMatrixSets/${tileMatrixSet.encodedId}", "text/html")}">${tileMatrixSet.identifier}</a></h2>
       </#list>
       </ul>
<#include "common-footer.ftl">
