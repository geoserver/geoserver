<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item active'>Collections</li>">
<#include "common-header.ftl">
  <#assign collection=model.collection>
  <h1 id="title">${collection.name.value}</h1>

  <#include "collection_include.ftl">
  
  <a href="">Collection items</a>

<#include "common-footer.ftl">
