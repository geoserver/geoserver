<#global pagetitle="Tile matrix sets">
<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item active'>"+pagetitle+"</li>">
<#include "common-header.ftl">

  <h1>${pagetitle}</h1>
  <p class="my-4">
    This document lists all the available tile matrix sets, linking to their definitions:<br/>
  </P>

  <ul>
  <#list model.tileMatrixSets as tileMatrixSet>
      <li><a href="${serviceLink("/tileMatrixSets/${tileMatrixSet.encodedId}", "text/html")}">${tileMatrixSet.id}</a></LI>
  </#list>
  </ul>
<#include "common-footer.ftl">
