<#include "symbol.ftl">
<#macro renderer(renderer)>
<li>Renderer:
        <ul>
            <#if renderer.type == "simple">
            <li>Simple Renderer:</li>
            
            <#call symbol(renderer.symbol)>
            <li>Label: ${renderer.label}</li>
            <li>Description: ${renderer.description}</li>
            </#if>
            <#if renderer.type == "classBreaks">
            <li>Class Breaks Renderer:</li>
            <li>Field: ${renderer.field}</li>
            <li>Min Value: ${renderer.minValue?string("#")}</li>
            <li>Class Break Infos:
                <ul>
                    <#list renderer.classBreakInfos as classBreak>
                    <li>Class Max Value: ${classBreak.classMaxValue?string("#")}</li>
                    <li>Label: ${classBreak.label}</li>
                    <li>Description: ${classBreak.description}</li>
                    <#call symbol(classBreak.symbol)>
                    </#list>
                </ul> 
            </li>
            
            </#if>
            <#if renderer.type == "uniqueValue">
            <li>Unique Value Renderer:</li>
            <li>Field1: ${renderer.field1}</li>
            <li>Field2: ${renderer.field2?if_exists}</li>
            <li>Field3: ${renderer.field3?if_exists}</li>
            <li>Field Delimiter: ${renderer.fieldDelimiter?if_exists}</li>
            <li>Default Symbol: ${renderer.defaultSymbol?if_exists}</li>
            <li>Default Label: ${renderer.defaultLabel?if_exists}</li>
            <li>Unique Value Infos:
            <ul>
            <#list renderer.uniqueValueInfos as uniqueValue>
                <li>Value: ${uniqueValue.value}</li>
                <li>Label: ${uniqueValue.label}</li>
                <li>Description: ${uniqueValue.description}</li>
                <#call symbol(uniqueValue.symbol)>
            </#list>
            </ul>
            </li>
            </#if>
            <!-- TODO unique values -->
        </ul>
    </li>
</#macro>