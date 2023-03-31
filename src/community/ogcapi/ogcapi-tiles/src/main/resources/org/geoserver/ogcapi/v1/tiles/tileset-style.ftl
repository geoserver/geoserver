<#global pagetitle=model.id>
<#global homeLink=serviceLink("")>
<#global collectionsLink=serviceLink("collections")>
<#global collectionLink=serviceLink("collections/" + model.id)>
<#global stylesLink=serviceLink("collections/" + model.id + "/styles")>
<#global tilesLink=serviceLink("collections/" + model.id + "/styles/" + model.styleId + "/map/tiles")>
<#global pagecrumbs=
    "<li class='breadcrumb-item'><a href='"+ homeLink +"'>Home</a></li>
    <li class='breadcrumb-item'><a href='" + collectionsLink +"'>Collections</a></li>
    <li class='breadcrumb-item'><a href='" + collectionLink  + "'>" + pagetitle+ "</a></li>
    <li class='breadcrumb-item'><a href='" + stylesLink + "'>styles</a></li>
    <li class='breadcrumb-item'>" + model.styleId + "</li><li class='breadcrumb-item'>map</li>
    <li class='breadcrumb-item'><a href='" + tilesLink + "'>tiles</a></li>
    <li class='breadcrumb-item'>" + model.tileMatrixId + "</li>">

<#include "common-header.ftl">
<#include "tileset-include.ftl">  
<#include "common-footer.ftl">

