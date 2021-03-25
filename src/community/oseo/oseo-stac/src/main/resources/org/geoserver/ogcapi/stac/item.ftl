<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item'><a href='"+serviceLink("collections")+"'>Collections</a></li><li class='breadcrumb-item'><a href='"+serviceLink("collections" + collection)+"'>"+collection+"</a></li><li class='breadcrumb-item active'><a href='" + serviceLink("collections/" + collection + "/items") + "'>items</a></li>">
<#include "common-header.ftl">
  <h2>${item.attributes.identifier.value}</h2>
  <#include "item_include.ftl">

<#include "common-footer.ftl">