<#include "head.ftl">

<#if properties.workspaceName != 'NO_WORKSPACE'>
    Workspace Name:  "${properties.workspaceName}"
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
            <#if el.language??>(${el.language}) </#if>
            <#if el.vocabulary??>[${el.vocabulary}] </#if>
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

<#if properties.authorityURLs != 'NO_AUTHORITY_URL'>
    <h4>Authority URLs for the root WMS Layer</h4>
    <ul>
        <#list properties.authorityURLs as a>
            <li>Name:  "${a.properties.name}" --> Link:  "${a.properties.href}"</li>
        </#list>
    </ul>
</#if>

<#if properties.identifiers != 'NO_IDENTIFIER'>
    <h4>Root Layer Identifiers</h4>
    <ul>
        <#list properties.identifiers as i>
            <li>Authority:  "${i.properties.authority}" --> Identifier:  "${i.properties.identifier}"</li>
        </#list>
    </ul>
</#if>

<#if properties.srsList != 'NO_SRSList'>
    <h4>Limited SRS List</h4>
    <ul>
        <li>${properties.srsList}</li>
    </ul>
</#if>

Output bounding box for every supported CRS:  "${properties.bboxForEachCRS}"

<h4>Raster Rendering Options</h4>
<ul>
  <li>Interpolation: "${properties.interpolation!}"</li>
</ul>

<h4>KML Options</h4>
<ul>
  <#if properties.kmlReflectorMode != 'NO_KMLREFLECTORMODE'>  
    <li>Default Reflector Mode:  "${properties.kmlReflectorMode!}"</li>
  </#if>
  <#if properties.kmlSuperoverlayMode != 'NO_KMLSUPEROVERLAY'>
    <li>Default Superoverlay Mode:  "${properties.kmlSuperoverlayMode!}"</li>
  </#if>
  <#if properties.kmlAttr != 'NO_KMLATTR'>
    <li>Generate vector placemarks:  "${properties.kmlAttr!}"</li>
  </#if>
  <#if properties.kmlPlacemark != 'NO_KMLPLACEMARK'>
    <li>Generate raster placemarks:  "${properties.kmlPlacemark!}"</li>
  </#if>
  <#if properties.kmlKmscore != 'NO_KMLKMSCORE'>
    <li>Raster/vector threshold:  "${properties.kmlKmscore!}"</li>
  </#if>
</ul>

<h4>Resource consumption limits</h4>
<ul>
  <li>Max rendering memory:  "${properties.maxRequestMemory}"</li>
  <li>Max rendering time:  "${properties.maxRenderingTime}"</li>
  <li>Max rendering errors:  "${properties.maxRenderingErrors}"</li>
</ul>

<h4>Watermark Settings</h4>
<ul>
  <li>Enable watermark:  "${properties.watermarkEnabled!}"</li>
  <#if properties.watermarkUrl != 'NO_WATERMARK_URL'>
    <li>Watermark URL:  "${properties.watermarkUrl!}"</li>
  </#if>
  <li>Watermark Transparency:  "${properties.watermarkTransparency!}"</li>
  <li>Watermark Position:  "${properties.watermarkPosition!}"</li>
</ul>

<h4>Image output options</h4>
<ul>
  <#if properties.pngCompression != 'null'>
    <li>PNG Compression:  "${properties.pngCompression}"</li>
  </#if>
  <#if properties.jpegCompression != 'null'>
    <li>JPEG Compression:  "${properties.jpegCompression}"</li>
  </#if>
  <li>SVG Producer:  "${properties.svgRenderer}"</li>
  <li>SVG Antialising:  "${properties.svgAntiAlias}"</li>
</ul>

<h4>WMS-Animator Options</h4>
<ul>
  <#if properties.maxAllowedFrames != 'null'>
    <li>Max allowed frames:  "${properties.maxAllowedFrames}"</li>
  </#if>
  <#if properties.maxAnimatorRenderingTime != 'null'>
    <li>Max rendering time(ms):  "${properties.maxAnimatorRenderingTime}"</li>
  </#if>
  <#if properties.maxRenderingSize != 'null'>
    <li>Max rendering size(bytes):  "${properties.maxRenderingSize}"</li>
  </#if>
  <#if properties.framesDelay != 'null'>
    <li>Frames delay(ms):  "${properties.framesDelay}"</li>
  </#if>
  <#if properties.loopContinuosly != 'null'>
    <li>Loop Continuously:  "${properties.loopContinuosly}"</li>
  </#if>
</ul>

<#include "tail.ftl">