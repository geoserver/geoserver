
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
    layout: "StandaloneLayout",
    oauth2RedirectUrl: document.getElementById('oauth2RedirectUrl').value,
    onComplete: () => {
      document.documentElement.classList.remove('dark-mode');
      const toggle = document.querySelector('.theme-switch');
      if (toggle) toggle.style.display = 'none';
    }
  });
});
