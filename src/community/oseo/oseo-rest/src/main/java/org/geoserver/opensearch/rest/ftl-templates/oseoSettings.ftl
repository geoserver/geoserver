<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
  <title>GeoServer OWS Configuration</title>
  <meta name="ROBOTS" content="NOINDEX, NOFOLLOW"/>
</head>
<body>

<#setting number_format="#0.0#">

<#if properties.workspace.name??>
    Workspace Name:  "${properties.workspace.name}"
</#if>

<h4>Service Metadata</h4>

<ul>
  <li>OSEO Enabled:  "${properties.enabled!}"</li>
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
            <#if el.language??>(${el.language!}) </#if>
            <#if el.vocabulary??>[${el.vocabulary!}] </#if>
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
<ul>
  <li>Attribution:  "${properties.attribution!}"</li>
  <li>Global Queryables:  "${properties.globalQueryables!}"</li>
</ul>


</body>
</html>