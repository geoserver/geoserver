<#global pagetitle=model.id>
<#global pagepath="/collections/"+model.id>
<#global pagecrumbs="<li class='breadcrumb-item'><a href='"+serviceLink("")+"'>Home</a></li><li class='breadcrumb-item'><a href='"+serviceLink("styles")+"'>Styles</a></li><li class='breadcrumb-item active'>"+model.id+"</li>">
<#include "common-header.ftl">

  <div class="card my-4">
    <div class="card-header">
      <h2>${model.id}</h2>
    </div>
    <div class="card-body">
      <ul>
        <li id="title"><b>Title</b>: ${model.title!"N/A"}</li>
        <li id="description"><b>Description</b>: ${model.description!"N/A"}</li>
        <li><b>Keywords</b>:
          <#if model.keywords?? && model.keywords?size gt 0>
            <ul>
              <#list model.keywords as kw>
                <li>${kw}</li>
              </#list>
            </ul>
          <#else>N/A
          </#if>
        </li>
        <li id="poc"><b>Point of contact</b>: ${model.pointOfContact!"N/A"}</li>
        <li><b>Access constraints</b>: ${model.accessConstraints!"N/A"}</li>
        <li><b>Dates</b>:
        <#if model.dates??>
          <ul>
            <li><b>Creation</b>: ${model.dates.creation!"N/A"}</li>
            <li><b>Publication</b>: ${model.dates.publication!"N/A"}</li>
            <li><b>Revision</b>: ${model.dates.revision!"N/A"}</li>
            <li><b>Valid till</b>: ${model.dates.validTill!"N/A"}</li>
            <li><b>Received on</b>: ${model.dates.receivedOn!"N/A"}</li>
          </ul>  
        <#else>N/A
        </#if>
        </li>
        <li id="stylesheets"><b>Stylesheets</b>:
            <ul>
              <#list model.stylesheets as ss>
            <li><a href="${ss.link.href}">${ss.title}</a> (${ss.native?string('native','converted')})</li>
          </#list>
            </ul>
        </li>
        <li id="layers"><b>Layers</b>:
          <ul>
            <#list model.layers as layer>
              <li>
                ${layer.id}: ${layer.type}. 
                <#if layer.sampleData??><br/>Sample data available:
                      <select onchange="window.open(this.options[this.selectedIndex].value);this.selectedIndex=0" >
                        <option value="none" selected>-- Please choose a format --</option>
                        <#list layer.sampleData as link>
                        <option value="${link.href}">${link.type}</option>
                        </#list>
                      </select>
                </#if>
                <br/>
                <#if layer.attributes?? && layer.attributes?size gt 0>
                    <b>Attributes used by the style:</b>
                    <ul>
                    <#list layer.attributes as attribute>
                      <li>${attribute.id}: ${attribute.type}</li>
                    </#list>
                    </ul>
                <#else>
                  The style does not use any attribute from this layer.    
                </#if>
              </li>   
            </#list>
          </ul>
        </li>
      </ul>
    </div>
  </div>
<#include "common-footer.ftl">
