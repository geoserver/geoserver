<#if model.title??> 
<#global pagetitle=model.title>
<#else>
<#global pagetitle=model.id>
</#if>
<#global pagecrumbs>
  <li class='breadcrumb-item'><a href='${serviceLink("")}'>Home</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("collections")}'>Collections</a></li>
  <li class='breadcrumb-item active'>${pagetitle}</li>
</#global>
<#include "common-header.ftl">

  <div class="row">
    <div class="col-xs col-md-6 col-lg-8">
      <div class="card my-4">
        <div class="card-header">
          <h1>${model.id}</h1>
        </div>
        <div class="card-body">
          <ul>
          <#if model.title??> 
            <li><b>Title</b>: <span id="${model.htmlId}_title">${model.title}</span><br/></li>
          </#if>
          <#if model.description??>
            <li><b>Description</b>: <span id="${model.htmlId}_description">${model.description!}</span><br/></li>
          </#if>
          <#assign spatial = model.extent.spatial>
            <li id="${model.htmlId}_spatial"><b>Geographic extents</b>:
              <ul>
              <#list spatial as se>
                <li>${se.getMinX()}, ${se.getMinY()}, ${se.getMinZ()}, ${se.getMaxX()}, ${se.getMaxY()}, ${se.getMaxZ()}.</li>
              </#list>
              </ul>
            </li>
          </ul>
          
          3D contents available for this collection:
          <ul>
          <#list model.content as content>
            <li><a href="${content.href}">${content.title}</a>
            <#if content.type == 'application/json+3dtiles'>
               <a id="${model.htmlId}_cesium" href="../cesium?url_3dtiles_json=${content.href}" class="badge">View in Cesium viewer</a>
            </#if>
            <#if content.type == 'application/json+i3s'>
                           <a id="${model.htmlId}_i3s" href="../i3s?i3s_resource_url=${content.href}" class="badge">View in i3s client</a>
                        </#if>
            </li> 
          </#list>
          </ul>
        </div>
      </div>
    </div>
  </div>


<#include "common-footer.ftl">
