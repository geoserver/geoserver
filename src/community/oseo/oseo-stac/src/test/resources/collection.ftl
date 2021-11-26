<#assign loadedJSON = "${loadJSON('readAndEval.json')}">
<#assign evalJSON = loadedJSON?eval_json>

<h1>This is a LANDSAT product!</h1>
<#list evalJSON as k, v>
    <h2>${k} => ${v}</h2>
</#list>