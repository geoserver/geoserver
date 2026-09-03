
window.addEventListener('load', function() {
  const inputs = document.getElementsByClassName('form-select-open-limit');
  for (let input of inputs) {
    input.addEventListener('change', function() {
      window.open(this.options[this.selectedIndex].value);
      this.selectedIndex = 0;
    });
  }
});
