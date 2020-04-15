<#macro color(c)>[${c[0]}, ${c[1]}, ${c[2]}, ${c[3]}]</#macro>
<#macro outline(o)>Outline:
   <ul>
       <li><i>Simple Line Symbol:</i></li>
       <li>Color: <#call color(o.color)>, Width: ${o.width}</li>
   </ul>
</#macro> 
<#macro symbol(symbol)>
<li>Symbol:
    <ul>
        <#if symbol.type == "esriSFS">
            <li><i>Simple Fill Symbol:</i></li>
            <li>Style: ${symbol.style}, Color: [${symbol.color[0]}, ${symbol.color[1]}, ${symbol.color[2]}, ${symbol.color[3]}]</li>
            <li><#call outline(symbol.outline)>
            </li>
        </#if>
        <#if symbol.type == "esriTS">
            <li><i>Simple Text Symbol:</i></li>
            <li>Color: [${symbol.color[0]}, ${symbol.color[1]}, ${symbol.color[2]}, ${symbol.color[3]}]</li>
            <#if symbol.backgroundColor??><li>Background Color: [${symbol.backgroundColor[0]}, ${symbol.backgroundColor[1]}, ${symbol.backgroundColor[2]}, ${symbol.backgroundColor[3]}]</li></#if>
            <#if symbol.borderLineColor??><li>Border Line Color: [${symbol.borderLineColor[0]}, ${symbol.borderLineColor[1]}, ${symbol.borderLineColor[2]}, ${symbol.borderLineColor[3]}]</li></#if>
            <#if symbol.verticalAlignment??><li>Vertical Alignment: ${symbol.verticalAlignment}</li></#if>
            <#if symbol.horizontalAlignment??><li>Horizontal Alignment: ${symbol.horizontalAlignment}</li></#if>
            <li>RightToLeft: ${symbol.rightToLeft?string}</li>
            <#if symbol.haloColor??><li>Halo Color: [${symbol.haloColor[0]}, ${symbol.haloColor[1]}, ${symbol.haloColor[2]}, ${symbol.haloColor[3]}]</li></#if>
            <#if symbol.haloSize??><li>Halo Size: ${symbol.haloSize}</li></#if>
            <#if symbol.font??>
                <li>Font Family: ${symbol.font.family}</li>
                <li>Font Size: ${symbol.font.size}</li>
                <li>Font Style: ${symbol.font.style}</li>
                <li>Font Weight: ${symbol.font.weight}</li>
                <#if symbol.font.decoration??><li>Font Decoration: ${symbol.font.decoration}</li></#if>
            </#if>
        </#if>
        <#if symbol.type == "esriSMS">
            <li><i>Simple Marker Symbol:</i></li>
            <li>Style: ${symbol.style}, Color: <#call color(symbol.color)>, Size: ${symbol.size}, Angle: ${symbol.angle}
            <li><#call outline(symbol.outline)>
            </li>
        </#if>
        <#if symbol.type == "esriPMS">
            <li><i>Picture Marker Symbol:</i><br>
                <img src="data:${symbol.contentType};base64,${symbol.imageData}"/>
            </li>
        </#if>
        <#if symbol.type == "esriSLS">
            <li><i>Simple Line Symbol:</i><br>
                <li>Style: ${symbol.style.style}, Color: <#call color(symbol.color)>, Width: ${symbol.width}</li>
            </li>
        </#if>
    </ul>
</li>
</#macro>