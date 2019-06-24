<#setting locale="en_US">
<html>
  <head>
      <title>${pagetitle}</title>
      <meta name="language" content="en-US">
      <meta name="description" content="OGC API provided by GeoServer">
      <link rel="stylesheet" href="${resourceLink("wfs3css/blueprint/screen.css")}" type="text/css" media="screen, projection" />
      <link rel="stylesheet" href="${resourceLink("wfs3css/blueprint/print.css")}" type="text/css" media="print" />
      <link rel="stylesheet" href="${resourceLink("wfs3css/geoserver.css")}" type="text/css" media="screen, projection" />
      <link rel="stylesheet" href="${resourceLink("wfs3css/blueprint/ie.css")}" type="text/css" media="screen, projection" />
      <link rel="canonical" type="text/html" href="${serviceLink("wfs3")+pagepath}"/>
  </head>
<body>
   <div id="header">
     <a href="${serviceLink("wfs3")}"></a>
   </div>
   <div id="crumbs">
   ${pagecrumbs}
   </div>

   <div id="content">