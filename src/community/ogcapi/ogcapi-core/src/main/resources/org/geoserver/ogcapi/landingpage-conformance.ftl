       <h2>Conformance</h2>
       <p>The <a id="htmlConformanceLink" href="${model.getLinkUrl('conformance', 'text/html')!}"> conformance page</a> provides a list of the conformance classes implemented by this service. 
       <br/> 
       This conformance page is also available as
       <#list model.getLinksExcept("conformance", "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.
       </p>
