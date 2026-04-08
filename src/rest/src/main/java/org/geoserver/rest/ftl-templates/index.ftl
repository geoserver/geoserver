<html>
<head>
<title> GeoServer Configuration API </title>
<meta name="ROBOTS" content="NOINDEX, NOFOLLOW"/>
</head>
<body>
<h2>GeoServer Configuration API</h2>
<#if links?size != 0>
<ul>
<#foreach link in links>
<li><a href="${page.pageURI(link)}">${link}</a></li>
</#foreach>
</ul>
<#else>
<p>There are no REST extensions installed.  If you expected some, please verify your installation (did you restart the server?).</p>
</#if>
</body>
</html>
