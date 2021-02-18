<#setting locale="en_US">
<!DOCTYPE html>
<html lang="en">
  <head>
      <#if model?? && model.htmlTitle?has_content>
        <title>${model.htmlTitle}</title>
      </#if>
      <link rel="stylesheet" href="${resourceLink("apicss/bootstrap.min.css")}" type="text/css" media="all" />
      <link rel="stylesheet" href="${resourceLink("apicss/geoserver.css")}" type="text/css" media="all" />
  </head>
<body>
  <header id="header">
    <a href="${serviceLink("")}"></a>
  </header>
  <main>
    <div id="content" class="container">