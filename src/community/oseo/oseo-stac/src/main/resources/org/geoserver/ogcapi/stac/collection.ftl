<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item'><a href='"+serviceLink("collections")+"'>Collections</a></li><li class='breadcrumb-item'>"+model.collection.name.value+"</li>">
<#include "common-header.ftl">
  <#assign collection=model.collection>
  <h1 id="title">${collection.name.value}</h1>

  <#include "collection_include.ftl">
  
  <#if collection.assets.value?has_content>
    <p><b>Assets:</p></b>
    <#assign amap = collection.assets.value?eval_json>
    <ul>
    <#list amap as k, v>
      <li><a href="${v.href}">${(v.title)!k}</a>
      </li>
    </#list>
    </ul>
  </#if> 

  <p>More resources:</p>
  <ul>
  <li><a href="${serviceLink("collections/${collection.name.value}/queryables")}">CQL filtering queryables</a></li>
  <li><a href="${serviceLink("collections/${collection.name.value}/sortables")}">Sortables</a></li>
  <li><a href="${serviceLink("collections/${collection.name.value}/items")}">Collection items</a>
  </ul>

<#include "common-footer.ftl">
