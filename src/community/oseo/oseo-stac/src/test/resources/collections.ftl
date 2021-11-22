<#assign loadedJSON = "${loadJSON('willBeReplaced')}">

<h1>Collections</h1>
<#list loadedJSON as k, v>
    <h2>${k} => ${v}</h2>
</#list>