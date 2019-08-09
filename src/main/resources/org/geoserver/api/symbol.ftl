<#macro symbol(symbol)>
<li>Symbol:
    <ul>
        <#if symbol.type == "esriSFS">
            <li><i>Simple Fill Symbol:</i></li>
            <li>Style: ${symbol.style}, Color: [${symbol.color[0]}, ${symbol.color[1]}, ${symbol.color[2]}, ${symbol.color[3]}]</li>
            <li>Outline:
                <ul>
                    <li><i>Simple Line Symbol:</i></li>
                    <li>Style: ${symbol.style}, Color: [${symbol.outline.color[0]}, ${symbol.outline.color[1]}, ${symbol.outline.color[2]}, ${symbol.outline.color[3]}], Width: ${symbol.outline.width}</li>
                </ul>
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
    </ul>
</li>
</#macro>