<#include "head.ftl">
<h2>About:</h2>
<#if values?is_collection>
<ul><#list values as status>
    <li><h3>Module name: ${status.name}</h3>
        <ul>
            <li><b>Module</b> : ${status.module}</li>
            <#if status.component??>
               <li><b>Component</b> : ${status.component}</li>
            </#if>
            <#if status.version??>
                <li><b>Version</b> : ${status.version}</li>
            </#if>
            <#if status.isEnabled??>
                <li><b>Enabled</b> : ${status.isEnabled}</li>
            </#if>
            <#if status.isAvailable??>
                <li><b>Available</b> : ${status.isAvailable}</li>
            </#if>
            <#if status.message??>
                <li><b>Message</b> : ${status.message!}</li>
            </#if>
        </ul>
    </li></#list>
</ul>
</#if>
<#include "tail.ftl">