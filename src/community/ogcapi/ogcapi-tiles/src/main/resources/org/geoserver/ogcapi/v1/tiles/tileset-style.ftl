<#global pagetitle=model.id>
<#global pagecrumbs>
  <li class='breadcrumb-item'><a href='${serviceLink("")}'>Home</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections")}'>Collections</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections/" + model.id)}'>${pagetitle}</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections/" + model.id + "/styles")}'>styles</a></li>
  <li class='breadcrumb-item'>${model.styleId}</li>
  <li class='breadcrumb-item'>map</li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections/" + model.id + "/styles/" + model.styleId + "/map/tiles")}'>tiles</a></li>
  <li class='breadcrumb-item active'>${model.tileMatrixId}</li>
</#global>
<#include "common-header.ftl">
<#include "tileset-include.ftl">  
<#include "common-footer.ftl">

