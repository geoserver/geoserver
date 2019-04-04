<#include "head.ftl">

<#if properties.workspace.name??>
    Workspace Name:  "${properties.workspace.name}"
</#if>

<h4>Service Metadata</h4>

<ul>
  <li>WMS Enabled:  "${properties.enabled!}"</li>
  <li>Strict CITE compliance:  "${properties.citeCompliant!}"</li>
  <li>Maintainer:  "${properties.maintainer!}"</li>
  <li>Online Resource:  "${properties.onlineResource!}"</li>
  <li>Title:  "${properties.title!}"</li>
  <li>Abstract:  "${properties.abstract!}"</li>
  <li>Fees:  "${properties.fees!}"</li>
  <li>Access Constraints:  "${properties.accessConstraints!}"</li>
  <#if properties.keywords?is_enumerable>
    <li>Keywords: "
        <#list properties.keywords as el>
            ${el.value!}
            <#if el.language??>(${el.language!}) </#if>
            <#if el.vocabulary??>[${el.vocabulary!}] </#if>
        <#if el_has_next>, </#if></#list>
    "</li>
  <#else>
    <li>Keywords: ""</li>
  </#if>
  <li>Name:  "${properties.name!}"</li>
  <#if properties.versions?is_enumerable>
    <li>Versions:  "${properties.versions!}"</li>
  <#else>
    <li>Versions: ""</li>
  </#if>
  <li>Schema Base URL:  "${properties.schemaBaseURL!}"</li>
  <li>Verbose Messages:  "${properties.verbose!}"</li>
</ul>

<#if properties.authorityURLs??>
    <h4>Authority URLs for the root WMS Layer</h4>
    <ul>
        <#list properties.authorityURLs as a>
            <li>Name:  "${a.properties.name!}" --> Link:  "${a.properties.href!}"</li>
        </#list>
    </ul>
</#if>

<#if properties.identifiers??>
    <h4>Root Layer Identifiers</h4>
    <ul>
        <#list properties.identifiers as i>
            <li>Authority:  "${i.properties.authority!}" --> Identifier:  "${i.properties.identifier!}"</li>
        </#list>
    </ul>
</#if>

<#if properties.srsList??>
    <h4>Limited SRS List</h4>
    <ul>
        <li>${properties.srsList!}</li>
    </ul>
</#if>

Output bounding box for every supported CRS:  "${properties.bBOXForEachCRS!}"

<h4>Raster Rendering Options</h4>
<ul>
  <li>Interpolation: "${properties.interpolation!}"</li>
</ul>

<h4>KML Options</h4>
<ul>
  <#if properties.kmlReflectorMode??>
    <li>Default Reflector Mode:  "${properties.kmlReflectorMode!}"</li>
  </#if>
  <#if properties.kmlSuperoverlayMode??>
    <li>Default Superoverlay Mode:  "${properties.kmlSuperoverlayMode!}"</li>
  </#if>
  <#if properties.kmlAttr??>
    <li>Generate vector placemarks:  "${properties.kmlAttr!}"</li>
  </#if>
  <#if properties.kmlPlacemark??>
    <li>Generate raster placemarks:  "${properties.kmlPlacemark!}"</li>
  </#if>
  <#if properties.kmlKmscore??>
    <li>Raster/vector threshold:  "${properties.kmlKmscore!}"</li>
  </#if>
</ul>

<h4>Resource consumption limits</h4>
<ul>
  <li>Max rendering memory:  "${properties.maxRequestMemory!}"</li>
  <li>Max rendering time:  "${properties.maxRenderingTime!}"</li>
  <li>Max rendering errors:  "${properties.maxRenderingErrors!}"</li>
</ul>

<h4>Watermark Settings</h4>
<ul>
  <li>Enable watermark:  "${properties.watermarkEnabled!}"</li>
  <#if properties.watermarkUrl??>
    <li>Watermark URL:  "${properties.watermarkUrl!}"</li>
  </#if>
  <li>Watermark Transparency:  "${properties.watermarkTransparency!}"</li>
  <li>Watermark Position:  "${properties.watermarkPosition!}"</li>
</ul>

<h4>Image output options</h4>
<ul>
  <#if properties.pngCompression??>
    <li>PNG Compression:  "${properties.pngCompression!}"</li>
  </#if>
  <#if properties.jpegCompression??>
    <li>JPEG Compression:  "${properties.jpegCompression!}"</li>
  </#if>
  <li>SVG Producer:  "${properties.svgRenderer!}"</li>
  <li>SVG Antialising:  "${properties.svgAntiAlias!}"</li>
</ul>

<h4>WMS-Animator Options</h4>
<ul>
  <#if properties.maxAllowedFrames??>
    <li>Max allowed frames:  "${properties.maxAllowedFrames!}"</li>
  </#if>
  <#if properties.maxAnimatorRenderingTime??>
    <li>Max rendering time(ms):  "${properties.maxAnimatorRenderingTime!}"</li>
  </#if>
  <#if properties.maxRenderingSize??>
    <li>Max rendering size(bytes):  "${properties.maxRenderingSize!}"</li>
  </#if>
  <#if properties.framesDelay??>
    <li>Frames delay(ms):  "${properties.framesDelay!}"</li>
  </#if>
  <#if properties.loopContinuosly??>
    <li>Loop Continuously:  "${properties.loopContinuosly!}"</li>
  </#if>
</ul>

<#include "tail.ftl">