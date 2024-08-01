$(document).ready(function() {
    $(this).on('dblclick', 'option', function() {
        var selectedAttribute = $(this).text();
        var formattedAttribute = '${' + selectedAttribute + '}';
        var textbox = $('#featureCaptionTemplate');
        var prevVal = textbox.val();
        textbox.val(prevVal + formattedAttribute);
    });
});