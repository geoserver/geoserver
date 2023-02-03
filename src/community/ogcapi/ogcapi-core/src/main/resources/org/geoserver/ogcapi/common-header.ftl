<#setting locale="en_US">
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
      <link rel="stylesheet" href="${resourceLink("apicss/geoserver.css")}" type="text/css" media="all" />
      <link rel="stylesheet" href="${resourceLink("apicss/treeview.css")}" type="text/css" media="all" />
  </head>
<body>
  <header id="header">
    <a href="${serviceLink("")}"></a>
    <#if model??>
       <div id="fetch"><span style="margin: 0.5em">Fetch this resource as:</span>
          <select class="form-select form-select-sm" onchange="window.open(this.options[this.selectedIndex].value);this.selectedIndex=0" >
            <option value="none" selected>-- Please choose a format --</option>
            <#list model.getLinksFor("alternate") as link>
            <option value="${link.href}">${link.type}</option>
            </#list>
          </select>
       </div>
     </#if>
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
