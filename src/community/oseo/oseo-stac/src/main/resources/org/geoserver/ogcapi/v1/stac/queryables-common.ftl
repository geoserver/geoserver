    <div class="card-body">
      <#if model.getProperties()??>
      <ul id="queryables">
      <#list model.getProperties() as name, definition>
        <li><b>${name}</b>: ${definition.getTitle()}</li>
      </#list>
      </ul>
      <#else>
      <div class="p-3 mb-2 bg-warning text-dark">No queryables found.</div>
      </#if>
    </div>
