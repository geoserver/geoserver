
window.addEventListener('load', function() {
  // Build a system
  window.ui = SwaggerUIBundle({
    url: document.getElementById('apiLocation').value,
    dom_id: "#swagger-ui",
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout"
  });
});
