<#setting locale="en_US">
<#setting time_zone="utc">
<#setting date_format="iso">
<#setting datetime_format="iso">
<!DOCTYPE html>
<html lang="en">
  <head>
      <#if pagetitle?? && pagetitle?has_content>
        <title>${pagetitle}</title> 
      <#elseif model?? && model.htmlTitle?has_content>
        <title>${model.htmlTitle}</title>
      </#if>
      <meta http-equiv="content-type" content="text/html; charset=${.output_encoding}">
      <link rel="stylesheet" href="${resourceLink("apicss/bootstrap.min.css")}" type="text/css" media="all" />
      <link rel="stylesheet" href="${resourceLink("css/geoserver.css")}" type="text/css" media="all" />
      <link rel="stylesheet" href="${resourceLink("apicss/ogcapi.css")}" type="text/css" media="all" />
      <link rel="stylesheet" href="${resourceLink("apicss/treeview.css")}" type="text/css" media="all" />
      <script src="${resourceLink('webresources/ogcapi/common.js')}"></script>
  </head>
<body>
  <header id="header" class="gs-header">
    <div class="gs-header-bar">
      <div class="gs-header-left">
        <a class="logo" href="${serviceLink("")}"></a>
      </div>
      <#if model??>
        <div class="gs-header-right" id="fetch"><span style="margin: 0.5em">Fetch this resource as:</span>
            <select class="form-select form-select-sm form-select-open-basic">
              <option value="none" selected>-- Please choose a format --</option>
              <#list model.getLinksFor("alternate") as link>
              <option value="${link.href}">${link.type}</option>
              </#list>
            </select>
        </div>
      </#if>
    </div>
  </header>
  <#if pagecrumbs??>
  <div id="breadcrumb" class="py-2 mb-4">
    <div class="container">
      <nav aria-label="breadcrumb">
        <ol class="breadcrumb mb-0">
          ${pagecrumbs}
        </ol>
      </nav>
    </div>
  </div>
  </#if>
  <main>
    <div id="content" class="container">
