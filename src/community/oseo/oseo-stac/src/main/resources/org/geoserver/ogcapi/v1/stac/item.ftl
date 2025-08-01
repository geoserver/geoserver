<#global pagecrumbs>
  <li class='breadcrumb-item'><a href='${serviceLink("")}'>Home</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections")}'>Collections</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections/" + collection)}'>${collection}</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections/" + collection + "/items")}'>Items</a></li>
  <li class='breadcrumb-item active'>${item.attributes.identifier.value}</li>
</#global>

<#include "common-header.ftl">
  <h2>${item.attributes.identifier.value}</h2>
  <#include "item_include.ftl">

  <#if item.assets.value?has_content>
    <p><b>Assets:</p></b>
    <#assign amap = item.assets.value?eval_json>
    <ul>
    <#list amap as k, v>
      <li><a href="${v.href}">${(v.title)!k}</a>
      </li>
    </#list>
    </ul>
  </#if> 

<#include "common-footer.ftl">
