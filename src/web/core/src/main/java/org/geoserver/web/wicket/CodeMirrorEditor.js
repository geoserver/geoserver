var textarea = document.getElementById('$componentId');
var editor = CodeMirror.fromTextArea(textarea, { 
    mode: '$mode',
    theme: 'default',
    lineWrapping: true,
    lineNumbers: true,
    extraKeys: {"Ctrl-Space": "autocomplete"}
});
editor.getWrapperElement().style.fontSize = "12px"; 
editor.refresh();
if(!document.gsEditors) {
    document.gsEditors = {};
}
document.gsEditors.$componentId = editor;
document.getElementById('cm_undo').onclick = function() {
    editor.execCommand('undo');
};
document.getElementById('cm_redo').onclick = function() {
    editor.execCommand('redo')
};
document.getElementById('cm_goto').onclick = function() {
    var line = Number(prompt("Jump to line:", "")) - 1;
    var lastLine = editor.lineCount() - 1;
    if (line && !isNaN(line)) {
      if(line > lastLine) {
          editor.setCursor({line: lastLine, ch: 0})
      } else if(line < 0) {
          editor.setCursor({line: 0, ch: 0});
      } else {
          editor.setCursor({line: line, ch: 0});
      }
    }
    editor.focus();
};
document.getElementById('cm_font_size').onchange = function() {
    var fontSize = document.getElementById('cm_font_size').value;
    editor.getWrapperElement().style.fontSize = fontSize+"px"; 
    editor.refresh();
}
document.getElementById('cm_reformat').onclick = function() {
    var start, end, i;
    if (editor.getSelection()) {
        start = editor.getCursor(true).line;
        end = editor.getCursor(false).line;
    } else {
        start = 0;
        end = editor.lineCount();
    }
    for(i = start; i<end; i++) {
        editor.indentLine(i);
    }
}
replaceSelection = function(repl) {
	start = editor.getCursor(true).line;
    editor.replaceSelection(repl);
	lines = repl.split(/\r\n|\r|\n/).length;
    for(i = start + 1; i < start + lines; i++) {
        editor.indentLine(i);
    }
}
// This comes from http://thereisamoduleforthat.com/content/making-div-fullscreen-and-restoring-it-its-original-position
// Does not work so commented out
/*
document.getElementById('cm_fullscreen').onclick = function() {
	div = $('#$container');
    if (!div.hasClass('fullscreen')) { // Going fullscreen:
    	alert("Sigh, can't make this work at all...");
      // Save current values.
      editor.beforeFullscreen = {
        parentElement: div.parent(),
        index: div.parent().children().index(div),
        x: $(window).scrollLeft(), y: $(window).scrollTop(),
      };

      // Set values needed to go fullscreen.
      $('body').append(div).css('overflow', 'hidden');
      div.addClass('fullscreen');
      window.scroll(0,0);
    } else { // Going back to normal:
      // Restore saved values.
      div.removeClass('fullscreen');
      if (editor.beforeFullscreen.index >= editor.beforeFullscreen.parentElement.children().length) {
    	  editor.beforeFullscreen.parentElement.append(div);
      } else {
    	  div.insertBefore(editor.beforeFullScreen.parentElement.children().get(editor.beforeFullscreen.index));
      }
      $('body').css('overflow', 'auto');
      window.scroll(editor.beforeFullscreen.x, editor.beforeFullscreen.y);
      editor.beforeFullScreen = null;
    }
  };
*/
