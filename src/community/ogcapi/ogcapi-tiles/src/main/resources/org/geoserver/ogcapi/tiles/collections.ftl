<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item active'>Collections</li>">
<#include "common-header.ftl">

  <h1>GeoServer Tiled Collections</h1>
  <p class="my-4">
    This document lists all the tiles collections available in the Tiles service.<br/>
  </p>
       
  <div class="row">
    <#list model.collections as collection>
    <div class="col-xs-12 col-md-6 col-lg-4 pb-4">
      <div class="card h-100">
        <div class="card-header">
          <h2><a id="html_${collection.htmlId}_link" href="${serviceLink("collections/${collection.encodedId}", "text/html")}">${collection.id}</a></h2>
        </div>
        <div class="card-body">
          <ul>
          <#if collection.title??> 
            <li><b>Title</b>: <span id="${collection.htmlId}_title">${collection.title}</span><br/></li>
          </#if>
          <#if collection.description??>
            <li><b>Description</b>: <span id="${collection.htmlId}_description">${collection.description!}</span><br/></li>
          </#if>
          <#assign spatial = collection.extent.spatial>
            <li><b>Geographic extents</b>:
              <ul>
              <#list spatial as se>
                <li>${se.getMinX()}, ${se.getMinY()}, ${se.getMaxX()}, ${se.getMaxY()}.</li>
              </#list>
              </ul>
            </li>
          </ul>
        </div>
      </div>
    </div>
    </#list>
  </div>
<#include "common-footer.ftl">
