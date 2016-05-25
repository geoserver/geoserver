<#--
/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
-->
<html>
<head>
<title> GeoGig Web API </title>
<meta name="ROBOTS" content="NOINDEX, NOFOLLOW"/>
</head>
<body>
<h2>Geogig repositories</h2>
<#if repositories?size != 0>
<ul>
<#foreach repo in repositories>
<li><a href="${page.pageURI(repo.repoName)}">${repo.repoName}</a> (${repo.id})</li>
</#foreach>
</ul>
<#else>
<p>There are no Geogig DataStores configured and enabled.</p>
</#if>
</body>
</html>
