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
    <script>
    window.onload = function() {

      // Build a system
      const ui = SwaggerUIBundle({
        url: "${serviceLink('ogc/features/api?f=application%2Fopenapi%2Bjson%3Bversion%3D3.0')}",
        dom_id: '#swagger-ui',
        deepLinking: true,
        presets: [
          SwaggerUIBundle.presets.apis,
          SwaggerUIStandalonePreset
        ],
        plugins: [
          SwaggerUIBundle.plugins.DownloadUrl
        ],
        layout: "StandaloneLayout"
      })

      window.ui = ui
    }
  </script>
  </body>
</html>
