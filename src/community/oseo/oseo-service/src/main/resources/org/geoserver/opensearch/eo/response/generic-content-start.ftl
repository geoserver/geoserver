<#assign a = model.attributes />
<entry>
    <id>${id}</id>
    <title>${title}</title>
    <dc:identifier>${title}</dc:identifier>
    <updated>${updated}</updated>
    <dc:date>${dcDate}</dc:date>
    <#if georssGeom??>
        <georss:where>
            ${georssGeom}
        </georss:where>
    </#if>
    <#if georssBox??>
        <georss:box>${georssBox}</georss:box>
    </#if>
    <summary type="html"><![CDATA[
