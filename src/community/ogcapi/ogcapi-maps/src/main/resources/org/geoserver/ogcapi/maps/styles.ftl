<#include "common-header.ftl">
       <h2>Styles for ${model.published.prefixedName()}</h2>
       <p>This document lists the styles available for the ${model.published.prefixedName()} collection:</p>
       <ul>
       <#list model.styles as style>
       <li><a href="${serviceLink("collections/${model.published.prefixedName()}/styles/${style.id}/map?f=html")}">${style.id}</a>: ${style.title!"N/A"}</li>
       </#list>
       </ul>
       <p>
              This document is also available as <#list model.getLinksExcept("alternate", "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.</p>
<#include "common-footer.ftl">
