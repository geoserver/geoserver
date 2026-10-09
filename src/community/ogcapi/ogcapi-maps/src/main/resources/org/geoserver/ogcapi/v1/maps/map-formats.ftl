<#-- Card footer offering the HTML map link and the other map formats. Callers assign mapFormatsResource (the style
     or the collection), mapFormatsRel (the link relation of its map) and mapFormatsLabel, plus mapFormatsLinkId
     when the HTML link needs an id. -->
<div class="card-footer">
  <div class="row">
    <div class="col-auto pe-0 py-1">
      ${mapFormatsLabel} <a<#if mapFormatsLinkId??> id="${mapFormatsLinkId}"</#if> class="btn btn-outline-primary btn-sm" href="${mapFormatsResource.getLinkUrl(mapFormatsRel, 'text/html')!}">HTML</a>
      or choose another format:
    </div>
    <div class="col-auto py-1">
      <select class="form-select form-select-sm form-select-open-limit">
        <option value="none" selected>-- Please choose a format --</option>
        <#list mapFormatsResource.getLinksExcept(mapFormatsRel, 'text/html') as link>
        <option value="${link.href}">${link.type}</option>
        </#list>
      </select>
    </div>
  </div>
</div>
