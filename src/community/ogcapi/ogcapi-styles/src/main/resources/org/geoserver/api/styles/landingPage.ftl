<#include "common-header.ftl">
   <div id="content">
       <h2>${service.title!"GeoServer Styles 1.0 Service"}</h2>
       <p>${service.abstract!""}<br/>
       This is the landing page of the Styles 1.0 service, providing links to the service API and its contents.
       <br/> 
       This document is also available as
       <#list model.getLinksExcept("landingPage", "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.
       </p>
       
       <h2>API definition</h2>
       <p>The <a id="htmlApiLink" href="${model.getLinkUrl('api', 'text/html')!}"> API document</a> provides a machine processable description of this service API conformant to OpenAPI 3. 
       <br/> 
       This API document is also available as
       <#list model.getLinksExcept("api", "application/json") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.
       </p>
       
       <h2>Styles</h2>
       <p>The <a id="htmlStylesLink" href="${model.getLinkUrl('styles', 'text/html')!}"> styles page</a> provides a list of all the styles available in this service. 
       <br/> 
       This styles page is also available as
       <#list model.getLinksExcept("styles", "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.
       </p>
       
       <h2>Contact information</h2>
       <ul>
       <li>Server managed by ${contact.contactPerson!"-unspecified-"}</li>
       <li>Organization: ${contact.contactOrganization!"-unspecified-"}</li>
       <li>Mail: <a href="mailto:${contact.contactEmail!''}">${contact.contactEmail!"-unspecified-"}</a></li>
       </ul> 
<#include "common-footer.ftl">