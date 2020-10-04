{
   "type":"Feature",
   "id":"http://www.geoserver.org/wfs/featureType/${featureType?js_string}",
   "properties":{
      <#if title?has_content>"title":"${title?js_string}",</#if>
      <#if abstract?has_content>"abstract":"${abstract?js_string}",</#if>
      "updated":"${now}",
      "offerings":[
         {
            "code":"http://www.opengis.net/spec/owc-geojson/1.0/req/wfs",
            "operations":[
               {
                  "code":"GetCapabilities",
                  "method":"GET",
                  "type":"application/xml",
                  "href":"${getCapabilitiesURL?js_string}"
               },
               {
                  "code":"GetFeature",
                  "method":"GET",
                  "type":"application/gml+xml",
                  "href":"${getFeatureURL?js_string}"
               }
            ]
         }
      ]
   }
}