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

  <#if model.getLinkUrl('prev', 'text/html')?? || model.getLinkUrl('next', 'text/html')??>
    <div>
    <nav>
      <ul class="pagination">
      <#if model.getLinkUrl('prev', 'text/html')??>
        <li class="page-item">
          <a class="page-link" id="prevPage" href="${model.getLinkUrl('prev', 'text/html')}">Previous page</a>
        </li>
      <#else>
        <li class="page-item disabled">
          <a class="page-link" id="prevPage" href="#" tabindex="-1" aria-disabled="true">Previous page</a>
        </li>
      </#if>
      <#if model.getLinkUrl('next', 'text/html')??>
        <li class="page-item">
          <a class="page-link" id="nextPage" href="${model.getLinkUrl('next', 'text/html')}">Next page</a>
        </li>
      <#else>
        <li class="page-item disabled">
          <a class="page-link" id="nextPage" href="#" tabindex="-1" aria-disabled="true">Next page</a>
        </li>
      </#if>
      </ul>
    </nav>
    </#if>

<#include "common-footer.ftl">
