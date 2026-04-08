/**
 * Augment Material theme version picker with external archive links.
 *
 * Material's version selector is rendered dynamically by the theme JS bundle
 * (it fetches versions.json at runtime). This script waits for the picker's
 * <ul class="md-version__list"> to appear in the DOM, then injects additional
 * entries pointing to the archived documentation on docs.geoserver.org.
 */
(function () {
  var archiveVersions = [
    { title: "2.28.x (Stable)",      url: "https://docs.geoserver.org/2.28.x/en/user/" },
    { title: "2.27.x (Maintenance)", url: "https://docs.geoserver.org/2.27.x/en/user/" },
    { title: "2.26.x",               url: "https://docs.geoserver.org/2.26.x/en/user/" },
    { title: "2.25.x",               url: "https://docs.geoserver.org/2.25.x/en/user/" },
    { title: "2.24.x",               url: "https://docs.geoserver.org/2.24.x/en/user/" },
    { title: "2.23.x",               url: "https://docs.geoserver.org/2.23.x/en/user/" },
    { title: "2.22.x",               url: "https://docs.geoserver.org/2.22.x/en/user/" },
    { title: "2.21.x",               url: "https://docs.geoserver.org/2.21.x/en/user/" },
    { title: "2.20.x",               url: "https://docs.geoserver.org/2.20.x/en/user/" },
    { title: "2.19.x",               url: "https://docs.geoserver.org/2.19.x/en/user/" }
  ];

  function augment(list) {
    if (list.dataset.archiveAugmented) return;
    list.dataset.archiveAugmented = "1";
    archiveVersions.forEach(function (v) {
      var li = document.createElement("li");
      li.className = "md-version__item";
      var a = document.createElement("a");
      a.href = v.url;
      a.className = "md-version__link";
      a.textContent = v.title;
      li.appendChild(a);
      list.appendChild(li);
    });
  }

  function tryAugment() {
    var list = document.querySelector(".md-version__list");
    if (list) { augment(list); return true; }
    return false;
  }

  function setup() {
    if (tryAugment()) return;
    // Version picker renders asynchronously after versions.json fetch;
    // observe the header until the list element appears.
    var header = document.querySelector(".md-header");
    if (!header) return;
    var observer = new MutationObserver(function () {
      if (tryAugment()) observer.disconnect();
    });
    observer.observe(header, { childList: true, subtree: true });
    // Safety disconnect after 5 s to avoid leaking the observer
    setTimeout(function () { observer.disconnect(); }, 5000);
  }

  // document$ is Material theme's RxJS Subject exposed globally;
  // it fires on initial load and every SPA navigation.
  if (typeof document$ !== "undefined") {
    document$.subscribe(setup);
  } else {
    document.addEventListener("DOMContentLoaded", setup);
  }
})();
