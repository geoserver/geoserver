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

  // Populate the icon grid by parsing .gs-icon-* classes from geoserver.css
  var grid = document.getElementById('icon-grid');
  var filterInput = document.getElementById('icon-filter');
  if (grid) {
    fetch('css/geoserver.css')
      .then(function(r) { return r.text(); })
      .then(function(css) {
        var seen = new Map();
        var re = /\.gs-icon-([\w-]+)\s*\{[^}]*url\(['"]?([^'")\s]+)['"]?\)/g;
        var m;
        while ((m = re.exec(css)) !== null) {
          var cls = 'gs-icon-' + m[1];
          if (!seen.has(cls)) {
            seen.set(cls, m[2].toLowerCase().endsWith('.svg'));
          }
        }

        var classes = Array.from(seen.keys()).sort();

        function renderTiles(filter) {
          grid.innerHTML = '';
          var term = filter ? filter.toLowerCase() : '';
          classes.forEach(function(cls) {
            if (term && cls.indexOf(term) === -1) return;
            var tile = document.createElement('div');
            tile.className = 'icon-tile';
            var icon = document.createElement('i');
            icon.className = 'gs-icon ' + cls;
            var label = document.createElement('span');
            label.textContent = '.' + cls;
            tile.appendChild(icon);
            tile.appendChild(label);
            if (seen.get(cls)) {
              var badge = document.createElement('span');
              badge.className = 'icon-badge';
              badge.textContent = 'svg';
              tile.appendChild(badge);
            }
            grid.appendChild(tile);
          });
          if (!grid.hasChildNodes()) {
            var msg = document.createElement('span');
            msg.style.color = 'var(--gs-secondary-color)';
            msg.style.fontSize = '0.875rem';
            msg.textContent = 'No icons match "' + filter + '".';
            grid.appendChild(msg);
          }
        }

        renderTiles('');

        if (filterInput) {
          filterInput.addEventListener('input', function() {
            renderTiles(filterInput.value.trim());
          });
        }
      })
      .catch(function() {
        grid.textContent = 'Icons require an HTTP server — open ui-components.html via http://, not file://.';
      });
  }
});
