<#ftl output_format="XML">
<#assign a = model.attributes />
<entry>
    <id>${oseoLink('search', 'parentId', a.parentIdentifier, 'uid', a.identifier.value, 'httpAccept', 'application/atom+xml')}</id>
    <title>${a.identifier.value}</title>
    <dc:identifier>${a.identifier.value}</dc:identifier>
    <updated>${updated}</updated>
    <dc:date>${dcDate}</dc:date>
    <#if a.footprint??>
        <georss:where>
            ${gml(a.footprint.rawValue)?no_esc}
        </georss:where>
        <georss:box>${minx(a.footprint.rawValue)} ${miny(a.footprint.rawValue)} ${maxx(a.footprint.rawValue)} ${maxy(a.footprint.rawValue)}</georss:box>
    </#if>
    <summary type="html"><![CDATA[
        <tr>
            <td valign="top" width="10%">
                <a href="${oseoLink('quicklook', 'parentId', a.parentIdentifier, 'uid', a.identifier)?no_esc}" target="_blank"
                   title="View browse image">
                    <img src="${oseoLink('quicklook', 'parentId', a.parentIdentifier, 'uid', a.identifier)?no_esc}"
                         align="left" border="0" height="66" hspace="8" width="66"/>
                </a>
            </td>
            <td valign="top" width="90%">
                <table>
                    <tr valign="top">
                        <td>
                            <b>Date</b>
                        </td>
                        <td> ${(a.timeStart.rawValue)!"unbounded"}/${(a.timeEnd.rawValue)!"unbounded"}</td>
                    </tr>
                    <tr valign="top">
                        <td>
                            <b>Media Type</b>
                        </td>
                        <td>
                            <a href="${oseoLink('search', 'parentId', a.parentIdentifier, 'uid', a.identifier, 'httpAccept', 'application/atom+xml')?no_esc}"
                               title="Atom format">ATOM</a>,
                            <a href="${oseoLink('search', 'parentId', a.parentIdentifier, 'uid', a.identifier, 'httpAccept', 'application/geo+json')?no_esc}"
                               title="GeoJSON format">JSON</a>
                        </td>
                    </tr>
                    <tr valign="top">
                        <td>
                            <b>Metadata</b>
                        </td>
                        <td>
                            <a href="${oseoLink('metadata', 'parentId', a.parentIdentifier, 'uid', a.identifier, 'httpAccept', 'application/gml+xml')?no_esc}"
                               title="O&M format">O&amp;M</a>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        ]]>
    </summary>
    <link href="${oseoLink('search', 'parentId', a.parentIdentifier, 'uid', a.identifier.value, 'httpAccept', 'application/atom+xml')}"
          rel="self" title="self" type="application/atom+xml"/>
    <link href="${oseoLink('metadata', 'parentId', a.parentIdentifier, 'uid', a.identifier.value, 'httpAccept', 'application/gml+xml')}"
          rel="alternate" title="O&amp;M metadata" type="application/gml+xml"/>
    <link href="${oseoLink('quicklook', 'parentId', a.parentIdentifier, 'uid', a.identifier.value)}" rel="icon"
          title="Quicklook" type="image/jpeg"/>
    <media:group>
        <media:content medium="image" type="image/jpeg"
                       url="${oseoLink('quicklook', 'parentId', a.parentIdentifier, 'uid', a.identifier.value)}">
            <media:category scheme="http://www.opengis.net/spec/EOMPOM/1.0">THUMBNAIL</media:category>
        </media:content>
    </media:group>
    <#list offerings as offer>
        <owc:offering code="${offer.offeringCode}">
            <#list 0..offer.offeringDetailList?size-1 as i>
                <owc:operation code="${offer.offeringDetailList[i].code}" href="${offer.offeringDetailList[i].href}"
                               method="${offer.offeringDetailList[i].method}"
                               type="${offer.offeringDetailList[i].type}"/>
            </#list>
        </owc:offering>
    </#list>
    <#if a.originalPackageType.value?has_content && a.originalPackageLocation.value?has_content>
        <link href="${baseURL}${a.originalPackageLocation.value?keep_after("BASE_URL}/")}" rel="enclosure"
              title="Source package download" type="${a.originalPackageType.value}"/>
    <#elseif a.originalPackageLocation.value?has_content>
        <link href="${baseURL}${a.originalPackageLocation.value?keep_after("BASE_URL}/")}" rel="enclosure"
              title="Source package download" type="application/octet-stream"/>
    </#if>
</entry>