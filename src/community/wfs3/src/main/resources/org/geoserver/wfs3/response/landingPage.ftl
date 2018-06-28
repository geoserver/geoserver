<html>
  <head>
      <link rel="stylesheet" href="${baseURL}wfs3css/blueprint/screen.css" type="text/css" media="screen, projection" />
      <link rel="stylesheet" href="${baseURL}wfs3css/blueprint/print.css" type="text/css" media="print" />
      <link rel="stylesheet" href="${baseURL}wfs3css/geoserver.css" type="text/css" media="screen, projection" />
      <link rel="stylesheet" href="${baseURL}wfs3css/blueprint/ie.css" type="text/css" media="screen, projection" />
  </head>
<body>
   <div id="header">
     <a href="${baseURL}"></a>
   </div>
   <div id="content">
       <h2>${service.title!"GeoServer WFS 3 Service"}</h2>
       <p>${service.abstract!""}</h2>
       <p>
       
       <h2>Landing page</h2>
       <p>This is the landing page of the WFS 3 service, providing links to the service API and its contents.
       <br/> 
       This document is also available as
       <#list model.getLinksExcept("landingPage", "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.
       </p>
       
       <h2>API definition</h2>
       <p>The <a id="jsonApiLink" href="${model.getLinkUrl('api', 'application/json')!}"> API document</a> provides a machine processable description of this service API conformant to OpenAPI 3. 
       <br/> 
       This API document is also available as
       <#list model.getLinksExcept("api", "application/json") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.
       </p>
       
       <h2>Collections</h2>
       <p>The <a id="htmlCollectionsLink" href="${model.getLinkUrl('collections', 'text/html')!}"> collection page</a> provides a list of all the collections available in this service. 
       <br/> 
       This collection page is also available as
       <#list model.getLinksExcept("collections", "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.
       </p>
       
       <h2>Contact information</h2>
       <ul>
       <li>Server managed by ${contact.contactPerson!"-unspecified-"}</li>
       <li>Organization: ${contact.contactOrganization!"-unspecified-"}</li>
       <li>Mail: <a href="mailto:${contact.contactEmail!''}">${contact.contactEmail!"-unspecified-"}</a></li>
       </ul> 
   </div>
</body>
</html>