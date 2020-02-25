<#include "head.ftl">
Transform: "${properties.name}" 
<ul>
<li>Source format: "${properties.sourceFormat}"</li>
<li>Output format: "${properties.outputFormat}"</li>
<li>File extension: "${properties.fileExtension}"</li>
<li>XSLT transform: <a href="${properties.name}.xslt">"${properties.xslt}"</a></li>
</ul>
<#include "tail.ftl">