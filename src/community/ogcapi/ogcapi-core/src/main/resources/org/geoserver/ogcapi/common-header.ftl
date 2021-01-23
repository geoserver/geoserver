<#setting locale="en_US">
<!DOCTYPE html>
<html lang="en">
  <head>
      <#if model?? && model.htmlTitle?has_content>
        <title>${model.htmlTitle}</title>
      </#if>
      <link rel="stylesheet" href="${resourceLink("apicss/blueprint/screen.css")}" type="text/css" media="screen" />
      <link rel="stylesheet" href="${resourceLink("apicss/blueprint/print.css")}" type="text/css" media="print" />
      <link rel="stylesheet" href="${resourceLink("apicss/geoserver.css")}" type="text/css" media="screen" />
      <link rel="stylesheet" href="${resourceLink("apicss/blueprint/ie.css")}" type="text/css" media="screen" />
  </head>
<body>
   <div id="header">
     <a href="${serviceLink("")}"></a>
   </div>
   <div id="content">