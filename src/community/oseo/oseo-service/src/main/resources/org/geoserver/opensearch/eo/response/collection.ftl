<#ftl output_format="XML">
<#setting number_format="#0.0#">
<#setting locale="en_US">
<#assign a = model.attributes />
<entry>
    <id>${oseoLink('search', 'uid', a.identifier.value, 'httpAccept', 'application/atom+xml')}</id>
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
        <tr valign="top">
            <td>
                <b>Title</b>
            </td>
            <td><#if a.title?? && a.title.value??>${a.title.value}<#else>${a.name.value}</#if></td>
        </tr>
        <tr valign="top">
            <td>
                <b>Description</b>
            </td>
            <td><#if a.description?? && a.description.value??>${a.description.value}<#else>No description available</#if></td>
        </tr>
        <tr valign="top">
            <td>
                <b>Type</b>
            </td>
            <td>Collection</td>
        </tr>
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
                <a href="${oseoLink('search', 'uid', a.identifier, 'httpAccept', 'application/atom+xml')?no_esc}"
                   title="ATOM format">ATOM</a>
                <a href="${oseoLink('search', 'uid', a.identifier, 'httpAccept', 'application/geo+json')?no_esc}"
                   title="GeoJSON format">JSON</a>
            </td>
        </tr>
        <tr valign="top">
            <td>
                <b>Metadata</b>
            </td>
            <td>
                <a href="${oseoLink('metadata', 'uid', a.identifier, 'httpAccept', 'application/vnd.iso.19139+xml')?no_esc}"
                   title="ISO format">ISO</a>
            </td>
        </tr>
        ]]>
    </summary>
    <link href="${oseoLink('search', 'uid', a.identifier.value, 'httpAccept', 'application/atom+xml')}" rel="self"
          title="self" type="application/atom+xml"/>
    <link href="${oseoLink('metadata', 'uid', a.identifier.value, 'httpAccept', 'application/vnd.iso.19139+xml')}"
          rel="alternate" title="ISO metadata" type="application/vnd.iso.19139+xml"/>
    <link href="${oseoLink('description', 'parentId', a.identifier.value)}" rel="search" title="Collection OSDD"
          type="application/opensearchdescription+xml"/>
    <#list offerings as offer>
        <owc:offering code="${offer.offeringCode}">
            <#list 0..offer.offeringDetailList?size-1 as i>
                <owc:operation code="${offer.offeringDetailList[i].code}" href="${offer.offeringDetailList[i].href}"
                               method="${offer.offeringDetailList[i].method}"
                               type="${offer.offeringDetailList[i].type}"/>
            </#list>
        </owc:offering>
    </#list>
</entry>