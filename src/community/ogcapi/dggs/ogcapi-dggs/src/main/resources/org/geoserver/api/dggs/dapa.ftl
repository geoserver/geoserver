<#include "common-header.ftl">
       <h2>${model.id} DAPA support</h2>
       <p>${model.description}</p>

       <h2>Variables</h2>
       <ul>
       <#list model.variables.variables as v>
         <li>${v.id}: ${v.description}</li>         
       </#list>
       </ul>
       
       <h2>Endpoints</h2>
       <p>The following analysis methods are supported on ${model.id}.</p>
       <#list model.endpoints as ep>
          <h3>${ep.id}</h3>
          <p>${ep.title?html}.<br/>${ep.description?html?replace("\n", "<br>")}<br>
          <a id="html_${ep.id}_link" href="${ep.getLinkUrl('ogc-dapa-endpoint', 'application/geo+json')!}">Run "${ep.id}" with default parameters</a>
          </p>
       </#list>
       
<#include "common-footer.ftl">
