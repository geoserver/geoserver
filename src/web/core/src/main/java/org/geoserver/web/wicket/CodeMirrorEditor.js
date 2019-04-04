function completeAfterKeyup(cm, pred) {
    if(cm.getOption("mode") && cm.getOption("mode").startsWith("text/sld")) {
      var cur = cm.getCursor();
      if (!pred || pred()) setTimeout(function() {
        if (!cm.state.completionActive)
          cm.showHint({completeSingle: false});
      }, 100);
    }
    return CodeMirror.Pass;
}

function completeIfValidPos(cm) {
    return completeAfterKeyup(cm, function() {
      var cur = cm.getCursor();
      var beforeCur = trimStart(cm.getRange(CodeMirror.Pos(cur.line, 0), cur));
      var afterCur = cm.getRange(cur, CodeMirror.Pos(cur.line, null));

      if (isEmpty(beforeCur) && isEmpty(afterCur)) return true;

      if ((isStartOpenTag(beforeCur) || isStartCloseTag(beforeCur)) && isEndTag(afterCur)) return true;

      return false
    });
}

function trimStart(str) {
  var startIndex = str.lastIndexOf('<');
  if (startIndex < 0) return "";

  return str.substring(startIndex);
}
function isStartOpenTag(str) {
   return /^<[^>]*$/.test(str);
}
function isStartCloseTag(str) {
  return /^<\/[^>\s]*$/.test(str);
}
function isEndTag(str) {
  return /^\s*>?\s*$/.test(str);
}
function isEmpty(str) {
  return str.trim() == "";
}

var textarea = document.getElementById('$componentId');
var editor = CodeMirror.fromTextArea(textarea, { 
    mode: '$mode',
    theme: 'default',
    lineWrapping: true,
    lineNumbers: true,
    extraKeys: {
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