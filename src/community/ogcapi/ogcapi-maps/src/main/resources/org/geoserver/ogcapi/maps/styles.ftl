<#include "common-header.ftl">
       <h2>Styles for ${model.published.prefixedName()}</h2>
       <p>This document lists the styles available for the ${model.published.prefixedName()} collection:</p>
       <ul>
       <hr/>
       <#list model.styles as style>
       <h3><#if style.style??>${style.id}<#else>Default style</#if></h3>
       
       <#if style.title??><p>${style.title}</p><#else>Default style</#if>
       <p>View as a <a href="${serviceLink("collections/${model.published.prefixedName()}/styles/${style.id}/map?f=html")}">HTML map</a></p>
       <hr/>
       </#list>
       </ul>
<#include "common-footer.ftl">
