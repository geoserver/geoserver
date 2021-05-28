<#global pagetitle=model.id>
<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item'><a href='" + serviceLink("collections")+"'>Collections</a></li><li class='breadcrumb-item'><a href='" + serviceLink("collections/" + model.id) + "'>" + pagetitle+ "</a></li><li class='breadcrumb-item'>tiles</li>">
<#include "common-header.ftl">
<#include "tiles-include.ftl">  
<#include "common-footer.ftl">
