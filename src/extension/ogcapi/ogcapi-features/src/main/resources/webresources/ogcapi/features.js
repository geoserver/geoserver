
window.addEventListener('load', function() {
  const inputs = document.getElementsByClassName('form-select-open-limit');
  for (let input of inputs) {
    input.addEventListener('change', function() {
      window.open(this.options[this.selectedIndex].value + '&limit='
        + document.getElementById('maxNumberOfFeaturesForPreview').value);
      this.selectedIndex = 0;
    });
  }
  const toggler = document.getElementsByClassName('caret');
  for (let item of toggler) {
    item.addEventListener('click', function() {
      this.parentElement.querySelector('.nested').classList.toggle('active');
      this.classList.toggle('caret-down');
    });
  }
});
