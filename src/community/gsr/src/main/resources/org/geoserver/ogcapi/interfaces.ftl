<p><b>Supported Interfaces: </b>
   <#list model.interfaces as interface>
   <a href="${serviceLink("${interface.path}")}" target="_blank">${interface.title}</a>
   </#list></p>
