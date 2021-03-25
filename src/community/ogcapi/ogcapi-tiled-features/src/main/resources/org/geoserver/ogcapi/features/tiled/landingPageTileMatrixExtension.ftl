       <h2>Tile matrix sets</h2>
       <p>Tiles are cached on <a id="tileMatrixSetsLink" href="${model.getLinkUrl('tileMatrixSets', 'text/html')!}">tile matrix sets</a>, defining tile layouts and zoom levels.
       <br/> 
       This page is also available as
       <#list model.getLinksExcept("tileMatrixSets", "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.
       </p> 
