<#setting locale="en_US">
<!DOCTYPE html>
<html lang="en">
  <head>
      <#if pagetitle?? && pagetitle?has_content>
        <title>${pagetitle}</title> 
      <#elseif model?? && model.htmlTitle?has_content>
        <title>${model.htmlTitle}</title>
      </#if>
      <link rel="stylesheet" href="${resourceLink("apicss/bootstrap.min.css")}" type="text/css" media="all" />
      <link rel="stylesheet" href="${resourceLink("apicss/geoserver.css")}" type="text/css" media="all" />
  </head>
<body>
  <header id="header">
    <a href="${serviceLink("")}"></a>
  </header>
  <#if pagecrumbs??>
    <div id="crumbs">
    ${pagecrumbs}
    </div>
  </#if>
  <main>
    <div id="content" class="container">
