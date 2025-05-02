<#global pagecrumbs>
  <li class='breadcrumb-item'><a href='${serviceLink("")}'>Home</a></li>
  <li class='breadcrumb-item active'>Processes</li>
</#global>
<#include "common-header.ftl">

  <h1>GeoServer Processes</h1>
  <p class="my-4">
    This document lists all the processes available in the OGC API Processes service.<br/>
  </p>
  
  <div class="row">
    <#list model.processes as process>
    <div class="col-xs-12 col-md-6 col-lg-4 pb-4">
      <div class="card h-100">
        <div class="card-header">
          <h2><a href="${serviceLink("processes/${process.id}")}">${process.id}</a></h2>
        </div>
        <#include "process_summary.ftl">
      </div>
    </div>
    </#list>
  </div>

<#include "common-footer.ftl">
