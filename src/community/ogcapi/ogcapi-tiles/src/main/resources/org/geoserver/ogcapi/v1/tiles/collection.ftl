<#if model.title??> 
<#global pagetitle=model.title>
<#else>
<#global pagetitle=model.id>
</#if>
<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item'><a href='"+serviceLink("collections")+"'>Collections</a></li><li class='breadcrumb-item'>"+pagetitle+"</li>">
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
            <li><b>Geographic extents</b>:
              <ul>
              <#list spatial as se>
                <li>${se.getMinX()}, ${se.getMinY()}, ${se.getMaxX()}, ${se.getMaxY()}.</li>
              </#list>
              </ul>
            </li>

            <#if model.queryable>
            <li>Queryables as <a id="html_${model.htmlId}_queryables" href="${model.getLinkUrl('queryables', 'text/html')!}">HTML</a>.</li>
            <#else>
            <li>Layer does not support filtering.</li>
            </#if>
          </ul>
          
          <#if model.mapTiles>
          <p><a href="${model.getLinkUrl('styles', 'text/html')!}">Styles available in this collection</a></p>
          </#if>

          Tiles available for this collection:
          <ul>
          <#if model.dataTiles>
            <li><a href="${model.getLinkUrl('tilesets-vector', 'text/html')!}">Data tiles</a></li> 
          </#if>
          <#if model.mapTiles>
            <li><a href="${model.getLinkUrl('tilesets-maps', 'text/html')!}">Map tiles in default style</a></li>
          </#if>
          </ul>
        </div>
      </div>
    </div>
  </div>


<#include "common-footer.ftl">
