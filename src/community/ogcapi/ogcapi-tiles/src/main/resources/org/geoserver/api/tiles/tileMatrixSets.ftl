<#include "common-header.ftl">
       <h2>Tile matrix sets</h2>
       <p>This document lists all the available tile matrix sets, linking to their definitions:<br/>
       <ul>
       <#list model.tileMatrixSets as tileMatrixSet>
           <li><a href="${serviceLink("/tileMatrixSets/${tileMatrixSet.encodedId}", "text/html")}">${tileMatrixSet.identifier}</a></h2>
       </#list>
       </ul>
<#include "common-footer.ftl">
