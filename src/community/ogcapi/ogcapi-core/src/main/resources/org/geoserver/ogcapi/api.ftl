<!-- HTML for static distribution bundle build -->
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <title>Swagger UI</title>
    <link rel="stylesheet" type="text/css" href="${resourceLink('swagger-ui/swagger-ui.css')}" >
    <link rel="icon" type="image/png" href="${resourceLink('swagger-ui/favicon-32x32.png')}" sizes="32x32" />
    <link rel="icon" type="image/png" href="${resourceLink('swagger-ui/favicon-16x16.png')}" sizes="16x16" />
    <style>
      html
      {
        box-sizing: border-box;
        overflow: -moz-scrollbars-vertical;
        overflow-y: scroll;
      }

      *,
      *:before,
      *:after
      {
        box-sizing: inherit;
      }

      body
      {
        margin:0;
        background: #fafafa;
      }
    </style>
  </head>

  <body>
    <div id="swagger-ui"></div>

    <script src="${resourceLink('swagger-ui/swagger-ui-bundle.js')}"> </script>
    <script src="${resourceLink('swagger-ui/swagger-ui-standalone-preset.js')}"> </script>
    <script src="${resourceLink('webresources/ogcapi/api.js')}"></script>
    <input type="hidden" id="apiLocation" value="${model.getApiLocation()}"/>
  </body>
</html>
