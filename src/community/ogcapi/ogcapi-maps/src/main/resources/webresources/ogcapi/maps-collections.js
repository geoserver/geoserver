/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

window.addEventListener('load', function() {
  const inputs = document.getElementsByClassName('form-select-open-limit');
  for (let input of inputs) {
    input.addEventListener('change', function() {
      window.open(this.options[this.selectedIndex].value);
      this.selectedIndex = 0;
    });
  }
});
