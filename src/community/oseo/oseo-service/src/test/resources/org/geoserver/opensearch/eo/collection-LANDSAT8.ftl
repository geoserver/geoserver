<#assign a = model.attributes /> 
 <tr valign="top">
    <td>
      <b>Title</b>
    </td>
    <td>This is the LS8 collection</td>
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
      <a href="${oseoLink('search', 'uid', a.identifier, 'httpAccept', 'application/atom+xml')}" title="ATOM format">ATOM</a>
      <a href="${oseoLink('search', 'uid', a.identifier, 'httpAccept', 'application/geo+json')}" title="GeoJSON format">JSON</a>
    </td>
  </tr>
  <tr valign="top">
    <td>
      <b>Metadata</b>
    </td>
    <td>
      <a href="${oseoLink('metadata', 'uid', a.identifier, 'httpAccept', 'application/vnd.iso.19139+xml')}" title="ISO format">ISO</a>
    </td>
  </tr>