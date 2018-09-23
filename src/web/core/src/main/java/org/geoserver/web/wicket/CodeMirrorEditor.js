function completeAfter(cm, pred) {
    if(cm.getOption("mode") && cm.getOption("mode").startsWith("text/sld")) {
        var cur = cm.getCursor();
        if (!pred || pred()) setTimeout(function() {
          if (!cm.state.completionActive)
            cm.showHint({completeSingle: false});
        }, 100);
        return CodeMirror.Pass;
     }
    
}
    
function completeIfAfterLt(cm) {
    if(cm.getOption("mode") && cm.getOption("mode").startsWith("text/sld")) {
        return completeAfter(cm, function() {
          var cur = cm.getCursor();
          return cm.getRange(CodeMirror.Pos(cur.line, cur.ch - 1), cur) == "<";
        });
    }
}
    
function completeIfInTag(cm) {
    if(cm.getOption("mode") && cm.getOption("mode").startsWith("text/sld")) {
        return completeAfter(cm, function() {
          var tok = cm.getTokenAt(cm.getCursor());
          if (tok.type == "string" && (!/['"]/.test(tok.string.charAt(tok.string.length - 1)) || tok.string.length == 1)) return false;
          var inner = CodeMirror.innerMode(cm.getMode(), tok.state).state;
          return inner.tagName;
        });
    }
}

var textarea = document.getElementById('$componentId');
var editor = CodeMirror.fromTextArea(textarea, { 
    mode: '$mode',
    theme: 'default',
    lineWrapping: true,
    lineNumbers: true,
    extraKeys: {
        "'<'": completeAfter,
        "'/'": completeIfAfterLt,
        "' '": completeIfInTag,
        "'='": completeIfInTag, 
        "Ctrl-Space": "autocomplete" 
    }
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
document.getElementById('cm_editor_heigth').onchange = function() {
    var height = document.getElementById('cm_editor_heigth').value;
    editor.setSize("100%", height); 
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
getSelection = function() {
	start = editor.getCursor(true).line;
    return editor.getSelection();
}