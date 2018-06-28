<#setting locale="en_US">
<html>
  <head>
      <link rel="stylesheet" href="${baseURL}wfs3css/blueprint/screen.css" type="text/css" media="screen, projection" />
      <link rel="stylesheet" href="${baseURL}wfs3css/blueprint/print.css" type="text/css" media="print" />
      <link rel="stylesheet" href="${baseURL}wfs3css/geoserver.css" type="text/css" media="screen, projection" />
      <link rel="stylesheet" href="${baseURL}wfs3css/blueprint/ie.css" type="text/css" media="screen, projection" />
  </head>
<body>
   <div id="header">
     <a href="${baseURL}"></a>
   </div>
   <div id="content">
       <h2>GeoServer WFS3 collections</h2>
       <p>This document lists all the collections available in the WFS 3 service.<br/>
       This document is also available as <#list model.getLinksExcept(null, "text/html") as link><a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.</p>
       
       <#list model.collections as collection>
       <a id="html_${collection.name}_link" href="${collection.getLinkUrl('items', 'text/html')!}&limit=${service.maxNumberOfFeaturesForPreview}"><h4>${collection.name}</h4></a>
       <p>
       <#if collection.title??> 
       <span id="${collection.name}_title">${collection.title}</span><br/>
       </#if>
       <#if collection.description??>
       <span id="${collection.name}_description">${collection.description!}</span><br/>
       </#if>
       <#assign se = collection.extent.spatial>
       Geographic extents: ${se.getMinX()}, ${se.getMinY()}, ${se.getMaxX()}, ${se.getMaxY()}.<br/>
       Collection items are also available in the following formats:
       <select onchange="window.open(this.options[this.selectedIndex].value + '&limit=${service.maxNumberOfFeaturesForPreview}');this.selectedIndex=0" >
       <option value="none" selected>--Please choose an option--</option>
       <#list collection.getLinksExcept("items", "text/html") as link>
       <option value="${link.href}">${link.type}</option>
       </#list>
       </select>
       </ul>
       </#list>
   </div>
</body>
</html>