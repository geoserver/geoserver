document.addEventListener('DOMContentLoaded', function() {
  document.getElementById('theme-light').addEventListener('click', function() {
    document.documentElement.setAttribute('data-gs-theme', 'light');
  });
  document.getElementById('theme-dark').addEventListener('click', function() {
    document.documentElement.setAttribute('data-gs-theme', 'dark');
  });
  document.querySelectorAll('.gs-switch-input').forEach(function(sw) {
    if (sw.id === 'sw1') {
      sw.addEventListener('change', function() {
        document.documentElement.setAttribute(
          'data-gs-theme',
          sw.checked ? 'dark' : 'light'
        );
      });
    }
  });
});
