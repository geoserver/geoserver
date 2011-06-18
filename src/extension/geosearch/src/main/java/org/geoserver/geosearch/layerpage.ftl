<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<#assign wmsUrl="../../wms?request=GetMap&version=1.1.1" 
         wfsUrl="../../gwc/service/wfs?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature"
         layersParam = "&layers="+name
         nameParam = "&typeName="+name
         kmlUrl="../../wms/kml?"
         gml2="GML2"
         gml2gzip="GML2-GZIP"
         gml3="gml3"
         json="json"
         PDFParam="&Format=application/pdf"
         bboxParam="&bbox="+bbox
         dimParams="&width="+width+"&height="+height
         srsParam="&srs="+srs
         imgUrl="../../images"
>
  <head>
    <title>${title} - Powered by GeoServer </title>
    <link type="image/gif" href="${imgUrl}/gs.gif" rel="icon"/>
    <link rel="stylesheet" type="text/css" href="../../openlayers/theme/default/style.css"/>
    <!-- Basic CSS definitions -->
    <style type="text/css">
        /* General settings */
        body {
            font-family: Verdana, Geneva, Arial, Helvetica, sans-serif;
            font-size: small;
        }
        
        /* The map and the location bar */
        #map {
            clear: both;
            position: relative;
            width: ${width}px;
            height: ${height}px;
            border: 1px solid black;
        }            
    </style>

  <style type="text/css"> 
/*-----------------------
General styles
-----------------------*/
body {
background: #fff;
color: #222;
font-family: Tahoma, "Lucida Sans Unicode", "Lucida Grande", Verdana, sans-serif;

}

h1, 
h2, 
h3, 
h4, 
h5, 
h6 {
color: #0082b6;
font-family: Tahoma, "Lucida Sans", "Lucida Sans Unicode", "Lucida Grande", Verdana, sans-serif;
margin: 0.5em 0;
}

h1 {
font-size: 1.7em;
margin: 0 0 0.5em;
}

h2 {
font-size: 1.3em;
border-bottom: 2px solid #000;
}

strong, 
em,
dt, 
b, 
i {
font-family: Tahoma, "Lucida Sans", "Lucida Sans Unicode", "Lucida Grande", Verdana, sans-serif;
}

a {
color: #0076a1;
text-decoration: none;
outline: none !important; /* avoid ugly dotted border for Firefox */
}

a:hover {
color: #cc6d00;
text-decoration: underline;
}

a img {
border: none; 
}

div {
outline: none !important; /* avoid ugly dotted border for Firefox, when listeners are added via js */
}


/*-----------------------
Utility Classes
-----------------------*/
.selfclear:after {
content: ".";
display: block;
height: 0;
clear: both;
visibility: hidden;
}

.selfclear {
display: inline-block; /* IE 7 */
}

.selfclear {
display: block;
}

* html .selfclear {
height: 1px; /* IE under 7 */
}

.leftwise {
float:left;
}

.rightwise {
float:right;
}

.nobreak {
white-space: nowrap;
}

.noshow {
display: none;
}

/*-*/
#geoserver-logo {
float: right;
width: 100px;
height: 30px;
overflow: hidden;
text-indent: -9999em;
background: url(${imgUrl}/powered-by-geoserver-100x30.png) no-repeat; 
}

/*-*/
#info {
color: #333;
font-size: 0.95em;
}

/*-*/
#view-data,
#download-data {
margin: 0 0 0.75em;
}

#view-data ul,
#view-data p,
#download-data ul,
#download-data p {
margin: 0;
padding: 0; 
float: left;
min-height: 20px;
}

#view-data ul li,
#download-data ul li {
list-style-type: none; 
float: left;
margin: 0 0.55em 0 0.3em;
padding: 0;
}

#view-data ul li a,
#download-data ul li a {
padding: 10px 0 10px 20px;
background: url(${imgUrl}/page_white_text.png) 0 50% no-repeat;
}

#view-data ul li a.pdf,
#download-data ul li a.pdf {
background: url(${imgUrl}/page_white_acrobat.png) 0 50% no-repeat; 
}

#view-data ul li a.google-earth,
#download-data ul li a.google-earth {
background: url(${imgUrl}/kml-16x16.png) 0 50% no-repeat; 
}

#view-data ul li a.kml,
#download-data ul li a.kml {
background: url(${imgUrl}/kml-16x16.png) 0 50% no-repeat; 
}

#view-data ul li a.shapefile,
#download-data ul li a.shapefile {
background: url(${imgUrl}/page_white_zip.png) 0 50% no-repeat; 
}

#view-data ul li a.json,
#download-data ul li a.json {
background: url(${imgUrl}/page_white_text.png) 0 50% no-repeat; 
}

#view-data ul li a.gml2,
#download-data ul li a.gml2 {
background: url(${imgUrl}/page_white_vector.png) 0 50% no-repeat; 
}

#view-data ul li a.gml3,
#download-data ul li a.gml3 {
background: url(${imgUrl}/page_white_vector.png) 0 50% no-repeat; 
}

#view-data ul li a.georss,
#download-data ul li a.georss {
background: url(${imgUrl}/feed.png) 0 50% no-repeat; 
}

/*-*/
#map {
clear: both;
position: relative;
width: 800px;
height: 317px;
border: 1px solid black;
}

/*-*/

#metadata table {
border-collapse: collapse;
border-spacing: 0;
border: 1px solid #000; 
}

#metadata th {
white-space: nowrap;
}

#metadata th,
#metadata td {
padding: 0.2em;
border: 1px solid #666;
}

/*-*/
#access-tiles dt {
font-weight: bold;
margin: 0;
padding: 0;
}

#access-tiles dd {
margin: 0 0 1em;
padding: 0;
}

#access-tiles dd p {
margin: 0 0 0.5em;
padding: 0;
}

code.example {
display: block;
font-size: 1.1em;
margin: 0.3em;
padding: 0.3em;
border: 1px solid #0082b6;
}

  </style>

    <script src="../../openlayers/OpenLayers.js" type="text/javascript">
    </script>
    <script defer="defer" type="text/javascript">

    var format = 'image/png';
    var untiled;
    var map;

    function init(){
        var bounds = new OpenLayers.Bounds(${bbox});

        var options = {
            controls: [],
            maxExtent: bounds,
            maxResolution: ${maxResolution?c},
            projection: '${srs}',
            units: 'degrees'
        };

        map = new OpenLayers.Map('map', options);
        
        // setup tiled layer
        tiled = new OpenLayers.Layer.WMS(
            "${name} - Tiled", "../../wms",
            {
                height: '${height?c}',
                width: '${width?c}',
                layers: '${name}',
                styles: '',
                srs: '${srs}',
                format: format,
                tiled: 'true',
                tilesOrigin : "${tilesOrigin}"
            },
            {buffer: 0} 
        );

        // setup single tiled layer
        untiled = new OpenLayers.Layer.WMS(
            "${name} - Untiled", "../../wms",
            {
                height: '${height?c}',
                width: '${width?c}',
                layers: '${name}',
                styles: '',
                srs: '${srs}',
                format: format
            },
            {singleTile: true, ratio: 1} 
        );

        map.addLayers([untiled, tiled]);

        // build up all controls            
        map.addControl(new OpenLayers.Control.PanZoom());
        map.addControl(new OpenLayers.Control.Navigation());
        map.addControl(new OpenLayers.Control.Scale($('scale')));
        map.addControl(new OpenLayers.Control.MousePosition({element: $('location')}));
        map.zoomToExtent(bounds);

    }
    </script>
  </head>
  <body onload="init()">
    <p>
    <a id="geoserver-logo" title="Powered by GeoServer" href="http://geoserver.org">Powered by GeoServer</a>
    </p>
    <h1>${title}</h1>
    <p id="info">${abstract}</p>

    <h2>Formats</h2>
    <div id="view-data" class="selfclear">
      <p>View the data as:</p>
      <ul>
        <li>
          <a class="pdf" href="${(wmsUrl + PDFParam + layersParam + bboxParam + '&styles=' + srsParam + dimParams)?html }">PDF</a>
        </li>
        <li>
          <a class="google-earth" href="${(kmlUrl + 'layers=' + name)?html}">Google Earth</a>
        </li>
      </ul><!-- /#view-data -->
    </div>
    <div id="download-data" class="selfclear">
      <p>Download the data as:</p>
      <ul>
        <li><a class="kml" href="${(kmlUrl + 'mode=download' + layersParam)?html }">KML</a></li>
        <li><a class="shapefile" href="${(wfsUrl + nameParam + '&outputFormat=' + 'SHAPE-ZIP')?html}">Shapefile</a></li>   
        <li><a class="json" href="${(wfsUrl + nameParam + '&outputFormat=' + json)?html}">JSON</a></li>
        <li><a class="gml2" href="${(wfsUrl + nameParam + '&outputFormat=' + gml2)?html}">GML2</a></li>
        <li><a class="gml3" href="${(wfsUrl + nameParam + '&outputFormat=' + gml3)?html}">GML3</a></li>
        <!-- 
           Problem: browser tries to up zip file as XML.
        <li><a href="${wfsUrl + nameParam + '&outputFormat=' + gml2gzip}">GML2</a> gZipped</li>
        -->
      </ul>
    </div><!-- /#download-data -->

    <h2>Map</h2>
    <div id="map"></div>

    <div id="Metadata">
      <h2>Metadata</h2>
      <table border="1">
        <tr>
          <th scope="row">Keywords</th>
          <td><#list keywords as keyword>${keyword}<#if keyword != keywords?last>, </#if></#list></td>
        </tr>
        <tr>
          <th scope="row">Extent</th>
          <td>${bbox}</td>
        </tr>
        <tr>
          <th scope="row">SRS</th>
          <td>${srs}</td>
        </tr>
        <tr>
          <th scope="row">Native CRS</th>
          <td>${nativeCRS}</td>
        </tr>
        <tr>
          <th scope="row">Declared CRS</th>
          <td>${declaredCRS}</td>
        </tr>
        <!--
        <#if metadataLinks?size != 0>
        <tr>
          <th scope="row">Metadata Links</th>
          <td><#list metadataLinks as link >${link}, </#list></td>
        </tr>
        </#if>
        -->
    </table>
    </div><!-- /#metadata -->    
    
    <#if gwc == "true">
    <div id="access-tiles">
      <h2>Access tiles</h2>
      Access the tiles, cached with GeoWebCache, through these API's:
      <dl>
        <dt>OpenLayers (<a href="http://geowebcache.org/trac/wiki/openlayers">Tutorial</a>)</dt>
        <dd>Use tiles in OpenLayers with the following code snippet:
          <code class="example">var layerstates = new OpenLayers.Layer.WMS(<br />
                  "${name} EPSG:4326 JPEG",<br />
                  "${gwcLink?html}service/wms",<br />
                  {layers: '${name}', format: 'image/jpeg'} );</code>
        </dd>
        <dt>Google Maps (<a href="http://geowebcache.org/trac/wiki/google_maps">Tutorial</a>)</dt>
        <dd>
         Use tiles in GoogleMaps with the following code snippet:
          <code class="example">tileUrlTemplate: '${(gwcLink + "service/gmaps?layers=${name}&zoom={Z}&x={X}&y={Y}")?html}',</code>
        </dd>
        <dt>Virtual Earth (<a href="http://geowebcache.org/trac/wiki/virtual_earth">Tutorial</a>)</dt>
        <dd>
          Use tiles in Virtual Earth with the following code snippet:
          <code class="example">var tileLayerURL = '${(gwcLink + "service/ve?quadkey=%4&format=image/png&layers=" + name)?html}';</code>
        </dd>
      </dl>
    </div>
    </#if>
    
  </body>
</html>
