<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:alt="http://www.opengis.net/alt/2.1"
      xmlns:atm="http://www.opengis.net/atm/2.1" xmlns:dc="http://purl.org/dc/elements/1.1/"
      xmlns:dct="http://purl.org/dc/terms/" xmlns:eo="http://a9.com/-/opensearch/extensions/eo/1.0/"
      xmlns:eop="http://www.opengis.net/eop/2.1" xmlns:geo="http://a9.com/-/opensearch/extensions/geo/1.0/"
      xmlns:georss="http://www.georss.org/georss" xmlns:gml="http://www.opengis.net/gml"
      xmlns:gs="http://www.geoserver.org/eo/test" xmlns:lmb="http://www.opengis.net/lmb/2.1"
      xmlns:media="http://search.yahoo.com/mrss/" xmlns:opt="http://www.opengis.net/opt/2.1"
      xmlns:os="http://a9.com/-/spec/opensearch/1.1/" xmlns:owc="http://www.opengis.net/owc/1.0"
      xmlns:sar="http://www.opengis.net/sar/2.1" xmlns:sch="http://www.ascc.net/xml/schematron"
      xmlns:ssp="http://www.opengis.net/ssp/2.1" xmlns:time="http://a9.com/-/opensearch/extensions/time/1.0"
      xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <os:totalResults>${totalResults}</os:totalResults>
    <os:startIndex>${startIndex}</os:startIndex>
    <os:itemsPerPage>${itemsPerPage}</os:itemsPerPage>
    <os:Query ${Query}/>
    <#if organization??>
        <author>
            <name>${organization}</name>
        </author>
    </#if>
    <#if title??>
        <title>${title}</title>
    </#if>
    <updated>${updated}</updated>
    <link ${self}/>
    <link ${first}/>
    <#if previous??>
        <link ${previous}/>
    </#if>
    <#if next??>
        <link ${next}/>
    </#if>
    <link ${last}/>
    <link ${search}/>
</feed>