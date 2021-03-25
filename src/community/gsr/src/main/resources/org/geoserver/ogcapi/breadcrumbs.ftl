   <h3 style="background-color: #E5EFF7;padding:3px">
   <#list model.path as link>
   <a href="${serviceLink("${link.path}/")}">${link.title}</a> <#if link_index lt model.path.size() - 1>&gt;</#if>
   </#list>
   </h3>