<#include "common-header.ftl">
   <div id="content">
       <h2>${service.title!"GeoServer Maps 1.0 Service"}</h2>
       <p>${service.abstract!""}<br/>
       This is the landing page of the Maps 1.0 service, providing links to the service API and its contents.
       </p>
       
       <h2>API definition</h2>
       <p>The <a id="htmlApiLink" href="${model.getLinkUrl('api', 'text/html')!}"> API document</a> provides a machine processable description of this service API conformant to OpenAPI 3.
       </p>
       
       <h2>Collections</h2>
       <p>The <a id="htmlCollectionsLink" href="${model.getLinkUrl('data', 'text/html')!}"> collection page</a> provides a list of all the collections available in this service. 
       </p>
       
       <#-- TODO when upgrading Freemaker add ?no_esc to avoid html escaping --> 
       ${htmlExtensions('landing')}
       
       <#include "landingpage-conformance.ftl">

       <h2>Contact information</h2>
       <ul>
       <li>Server managed by ${contact.contactPerson!"-unspecified-"}</li>
       <li>Organization: ${contact.contactOrganization!"-unspecified-"}</li>
       <li>Mail: <a href="mailto:${contact.contactEmail!''}">${contact.contactEmail!"-unspecified-"}</a></li>
       </ul> 
<#include "common-footer.ftl">