<#global pagecrumbs>
  <li class='breadcrumb-item active'>Home</li>
</#global>
<#include "common-header.ftl">

  <h1>${service.title!"GeoServer Processes 1.0 Service"}</h1>
  <p class="my-4">
    ${service.abstract!""}<br/>
    This is the landing page of the Processes 1.0 service, providing links to the service API and its contents.<br/>
  </p>

  <div class="row my-3">
    <div class="col-6 col-xl-3 mb-3">
      <div class="card h-100">
        <div class="card-body">
          <h2>API definition</h2>
          <p>The <a id="htmlApiLink" href="${model.getLinkUrl('api', 'text/html')!}"> API document</a> provides a machine processable description of this service API conformant to OpenAPI 3.
        </div>
      </div>
    </div>

    <div class="col-6 col-xl-3 mb-3">
      <div class="card h-100">
        <div class="card-body">
          <h2>Processes</h2>
          <p>The <a id="htmlProcessesLink" href="${model.getLinkUrl('processes', 'text/html')!}"> processes page</a> provides a list of all the processes available in this service.
          <br/>
        </div>
      </div>
    </div>

    <div class="col-6 col-xl-3 mb-3">
      <div class="card h-100">
        <div class="card-body">
          <#include "landingpage-conformance.ftl">
        </div>
      </div>
    </div>

  </div>

  <h2>Contact information</h2>
  <address>
    <ul>
      <li>Server managed by ${contact.contactPerson!"-unspecified-"}</li>
      <li>Organization: ${contact.contactOrganization!"-unspecified-"}</li>
      <li>Mail: <a href="mailto:${contact.contactEmail!''}">${contact.contactEmail!"-unspecified-"}</a></li>
    </ul>
  </address>
<#include "common-footer.ftl">