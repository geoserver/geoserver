
window.onload = function() {
    document.getElementById('submitButton').onclick = function() {
        if (document.frm.url.value == '') {
            alert('Please provide a URL before you submit this form!');
        } else {
            document.frm.submit();
        }
    };
    document.getElementById('clearButton').onclick = function() {
        document.frm.body.value = '';
    };
};
