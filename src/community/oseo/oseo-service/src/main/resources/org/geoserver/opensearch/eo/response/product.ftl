<#assign a = model.attributes />
<tr>
    <td valign="top" width="10%">
        <a href="${oseoLink('quicklook', 'parentId', a.parentIdentifier, 'uid', a.identifier)}" target="_blank" title="View browse image">
            <img src="${oseoLink('quicklook', 'parentId', a.parentIdentifier, 'uid', a.identifier)}" align="left" border="0" height="66" hspace="8" width="66"/>
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
                    <a href="${oseoLink('search', 'parentId', a.parentIdentifier, 'uid', a.identifier, 'httpAccept', 'application/atom+xml')}" title="Atom format">ATOM</a>,
                    <a href="${oseoLink('search', 'parentId', a.parentIdentifier, 'uid', a.identifier, 'httpAccept', 'application/geo+json')}" title="GeoJSON format">JSON</a>
                </td>
            </tr>
            <tr valign="top">
                <td>
                    <b>Metadata</b>
                </td>
                <td>
                    <a href="${oseoLink('metadata', 'parentId', a.parentIdentifier, 'uid', a.identifier, 'httpAccept', 'application/gml+xml')}" title="O&M format">O&amp;M</a>
                </td>
            </tr>
        </table>
    </td>
</tr>