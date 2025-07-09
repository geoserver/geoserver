    <div class="card-body">
      <#if model.getProperties()??>
      <ul id="queryables">
      <#list model.getProperties() as name, definition>
	    <li><b>${name}</b>
	        <#if definition.getDescription()??>
	            : ${definition.getDescription()}
                </#if>
            </li>
      </#list>
      </ul>
      <#else>
      <div class="p-3 mb-2 bg-warning text-dark">No queryables found.</div>
      </#if>
    </div>
