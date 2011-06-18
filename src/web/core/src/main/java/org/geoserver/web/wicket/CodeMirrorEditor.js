var textarea = document.getElementById('$componentId');
var editor = CodeMirror.fromTextArea("$componentId", { 
    height: "450px",
    content: textarea.value,
    tabMode: "shift",
    parserfile: "$syntax",
    lineNumbers: true, 
    textWrapping: false,
    disableSpellcheck: true,
    stylesheet: "$stylesheet",
    path: "./resources/org.geoserver.web.wicket.CodeMirrorEditor/js/codemirror/js/",
    initCallback: function(ed) {
       ed.win.document.body.style.fontSize = 12;
    }
});
if(!document.gsEditors) {
	document.gsEditors = {};
}
document.gsEditors.$componentId = editor;
document.getElementById('cm_undo').onclick = function() {
	editor.undo();
};
document.getElementById('cm_redo').onclick = function() {
	editor.redo();
};
document.getElementById('cm_goto').onclick = function() {
    var line = Number(prompt("Jump to line:", ""));
    var lastLine = editor.lineNumber(editor.lastLine());
    if (line && !isNaN(line)) {
      if(line > lastLine) {
    	  editor.jumpToLine(lastLine)
      } else if(line < 1) {
    	  editor.jumpToLine(1)
      } else {
    	  editor.jumpToLine(line);
      }
    }
};
document.getElementById('cm_font_size').onchange = function() {
	var fontSize = document.getElementById('cm_font_size').value;
	editor.lineNumbers.childNodes[0].style.fontSize = fontSize + "px";
	editor.win.document.body.style.fontSize = fontSize;
}
document.getElementById('cm_reformat').onclick = function() {
	if(editor.selection()) {
		editor.reindentSelection();
	} else {
		editor.reindent();
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