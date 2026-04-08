<#global pagetitle=model.id>
<#global pagepath="/processes/"+model.id>
<#global pagecrumbs>
  <li class='breadcrumb-item'><a href='${serviceLink("")}'>Home</a></li>
  <li class='breadcrumb-item'><a href='${serviceLink("processes")}'>Processes</a></li>
  <li class='breadcrumb-item active'>${model.id}</li>
</#global>
<#include "common-header.ftl">

  <div class="row">
    <div class="col-xs col-md-6 col-lg-8">
      <div class="card my-4">
        <div class="card-header">
          <h2>${model.id}</h2>
        </div>
        <#assign process=model>
        <#include "process_summary.ftl">
      </div>

     <div class="card my-4">
        <div class="card-header">
          <h2>Process inputs</h2>
        </div>
        <div class="card-body">
          <#if model.inputs??>
          <table class="function-table">
          <tr>
            <th>Identifier</th>
            <th>Type</th>
            <th>Min/Max</th>
            <th>Description</th>
            <th>Encodings</th>
          </tr>
          <#list model.inputs as id, parameter>
            <tr id="input_${id}">
                <td>${id}</td>
                <td>${parameter.schema.getTitle()!""}</td>
                <td>${parameter.minOccurs}/${parameter.maxOccurs}</td>
                <td>${parameter.description!""}</td>
                <td>
                  <#if parameter.encodings?has_content>
                    <div class="d-flex flex-wrap gap-1">
                    <#list parameter.encodings as encoding>
                      <span class="badge bg-process-inputs text-dark">${encoding}</span>
                    </#list>
                    </div>
                  </#if>
                </td>
            </tr>
          </#list>
          </table>
          <#else>
          The process has no inputs.
          </#if>
        </div>
      </div>

      <div class="card my-4">
        <div class="card-header">
          <h2>Process outputs</h2>
        </div>
        <div class="card-body">
          <#if model.outputs??>
          <table class="function-table">
          <tr>
            <th>Identifier</th>
            <th>Type</th>
            <th>Description</th>
            <th>Encodings</th>
          </tr>
          <#list model.outputs as id, parameter>
            <tr id="output_${id}">
                <td>${id}</td>
                <td>${parameter.schema.getTitle()!""}</td>
                <td>${parameter.description!""}</td>
                <td>
                  <#if parameter.encodings?has_content>
                    <div class="d-flex flex-wrap gap-1">
                    <#list parameter.encodings as encoding>
                      <span class="badge bg-process-inputs text-dark">${encoding}</span>
                    </#list>
                    </div>
                  </#if>
                </td>
            </tr>
          </#list>
          </table>
          <#else>
          The process has no outputs.
          </#if>
        </div>
      </div>
    </div>

    
  </div>
<#include "common-footer.ftl">
