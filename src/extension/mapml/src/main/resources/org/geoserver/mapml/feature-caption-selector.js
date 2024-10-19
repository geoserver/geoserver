function insertTextAtCaret(text) {
    var template = $('#featureCaptionTemplate');
    var start = template[0].selectionStart;
    var end = template[0].selectionEnd;

    // Get the text before and after the caret position
    var beforeText = template.val().substring(0, start);
    var afterText = template.val().substring(end);

    // Insert the text at the caret position
    template.val(beforeText + text + afterText);

    // Move the caret to the end of the inserted text
    var newCaretPosition = start + text.length;
    template[0].selectionStart = template[0].selectionEnd = newCaretPosition;

    // Set focus back to the textarea
    template.focus();
}

$(document).ready(function() {
    $(this).on('dblclick', 'option', function() {
        var selectedAttribute = $(this).text();
        var formattedAttribute = '${' + selectedAttribute + '}';
        insertTextAtCaret(formattedAttribute);
    });
});

