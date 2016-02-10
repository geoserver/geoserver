<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <title>GeoServer Resource Directory</title>
    <meta name="ROBOTS" content="NOINDEX, NOFOLLOW"/>
</head>
<body>

<h1>Resource Directory '${properties.path}'</h1>

<ul>
  <li>Name: '${properties.name}'</li>
  <li>Parent: <a href="${properties.parent_href}">${properties.parent_path}</a></li>
  <li>Last modified: ${properties.lastModified}</li>
  <li>Children:
	<ul>
	<#list properties.children as l>
	  <li><a href="${l.properties.href}">${l.properties.name}</a></li>
	</#list>
	</ul>
  </li>
</ul>

</body>
</html>
