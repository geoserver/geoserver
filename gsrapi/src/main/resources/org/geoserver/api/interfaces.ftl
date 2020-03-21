<p><b>Supported Interfaces: </b>
   <#list model.interfaces as interface>
   <a href="${serviceLink("gsr/services/${interface.path}")}" target="_blank">${interface.title}</a> 
   </#list></p>
