<#include "common-header.ftl">
       <h2>${model.id}</h2>
       <p><b>Title</b>: ${model.title!"N/A"}</p>
       <p><b>Description</b>: ${model.description!"N/A"}</p>
       <p><b>Keywords</b>:
         <#if model.keywords?? && model.keywords?size gt 0>
           <ul>
             <#list model.keywords as kw>
               <li>${kw}</li>
             </#list>
           </ul>
         <#else>N/A
         </#if>
       </p>
       <p><b>Point of contact</b>: ${model.pointOfContact!"N/A"}</p>
       <p><b>Access constraints</b>: ${model.accessConstraints!"N/A"}</p>
       <p><b>Dates</b>:
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
       </p>
       <p><b>Stylesheets</b>:
          <ul>
            <#list model.stylesheets as ss>
           <li><a href="${ss.link.href}">${ss.title}</a> (${ss.native?string('native','converted')})</li>
         </#list>
          </ul>
       </p>
       <p><b>Layers</b>:
         <ul>
           <#list model.layers as layer>
            <li>
               ${layer.id}: ${layer.type}. 
               <#if layer.sampleData??><a href="${layer.sampleData.href}">Sample data available</a>.</#if><br/>
               <#if layer.attributes?? && layer.attributes?size gt 0>
                  <p>Attributes used by the style:
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
      </p>
<#include "common-footer.ftl">
