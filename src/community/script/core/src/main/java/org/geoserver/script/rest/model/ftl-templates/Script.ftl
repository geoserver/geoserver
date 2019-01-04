<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
    <head>
        <title>Scripts</title>
    </head>
    <body>
        <h2>Scripts:</h2>
        <ul>
            <#foreach link in links>
            <li><a href="${script.pageURI(link)}">${link}</a></li>
            </#foreach>
        </ul>
    </body>
</html>