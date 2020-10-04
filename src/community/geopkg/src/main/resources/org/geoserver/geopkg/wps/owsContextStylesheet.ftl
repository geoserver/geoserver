{
   "type":"FeatureCollection",
   "id": "http://www.geoserver.org/wps/geopkg/styleableLayerSet/${groupId}",
   "properties":{
      "lang":"en",
      "title":"${groupTitle}",
      "updated":"${now}",
      "generator":"GeoServer",
      <#if contact.contactPerson?has_content || contact.contactEmail?has_content>
      "authors":[
                  {
                      <#if contact.contactPerson?has_content>"name":"${contact.contactPerson?js_string}",</#if>
                      <#if contact.contactEmail?has_content>"email":"${contact.contactEmail?js_string}"</#if>
                  }
              ],
      </#if>
      "links":[
          {
              "rel":"profile",
              "href":"http://www.opengis.net/spec/owc-geojson/1.0/req/core",
              "title":"This file is compliant with version 1.0 of OGC Context"
          }
      ]
   },
   "features":[
      <#assign count = 0>
      <#list layers as layer>
      {
         "type":"${layer.type}",
         "id":"http://www.opengis.net/spec/owc-json/1.0/req/gpkg/${layer.id}",
         "properties":{
            "title":"${layer.title}",
            "updated":"${now}",
            "active":true,
            "offerings":[
               {
                  "code":"http://www.opengis.net/spec/owc-json/1.0/req/gpkg/1.2/opt/features",
                  "operations":[
                     {
                        "code":"GPKG",
                        "method":"SELECT",
                        "type":"SQL Record Set",
                        "href":"${packageName}.gpkg",
                        "request": {
                           "type":"SQL Record Set",
                           "content":"SELECT * FROM ${layer.tableName};"
                        }
                     }
                  ],
                "styles":[
                    {
                        "name":"${layer.styleId}",
                        <#if layer.styleTitle?has_content>"title":"${layer.styleTitle}",</#if>
                        <#if layer.styleAbstract?has_content>"abstract":"${layer.styleAbstract}",</#if>
                        "default":true,
                        "content":{
                           "type":"SQL Record Set",
                           "content":"SELECT stylesheet, format FROM gpkgext_stylesheets WHERE style_id = (SELECT id FROM gpkgext_styles where style = '${layer.styleId}');"
                        }
                    }
                 ]
               }
            ]
         }
      }<#assign count = count + 1><#if count < layers?size>,</#if>
      </#list>
   ]
}