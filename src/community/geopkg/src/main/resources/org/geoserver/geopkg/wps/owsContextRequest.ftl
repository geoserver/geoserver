{
    "type":"FeatureCollection",
    "id": "http://www.geoserver.org/wps/geopkg/execute/${uuid}",
    "properties":{
        "lang":"en",
        "title":"GeoPackage download request information",
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
        {
            "type":"Feature",
            "id": "http://www.geoserver.org/wps/geopkg/execute/request/${uuid}",
            "properties":{
                "title":"WPS",
                "updated":"${now}",
                "offerings":[
                    {
                        "code":"http://www.opengis.net/spec/owc-geojson/1.0/req/wps",
                        "operations":[
                            {
                                "code":"GetCapabilities",
                                "method":"GET",
                                "type":"application/xml",
                                "href":"${getCapabilitiesURL?js_string}"
                            },
                            {
                                "code":"DescribeProcess",
                                "method":"GET",
                                "type":"application/xml",
                                "href":"${describeProcessURL?js_string}"
                            },
                            {
                                "code":"Execute",
                                "method":"POST",
                                "type":"application/xml",
                                "href":"${executeURL}",
                                "request":{
                                    "type":"application/xml",
                                    <#-- Freemarker has trouble json escaping this string, it's done in Java using Jackson -->
                                    "content" : ${executeBody}
                                }
                            }
                        ]
                    }
                ]
            }
        }
    ]
}
