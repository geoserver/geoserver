/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

function isCharacterKeyPress(evt) {
    if (typeof evt.which == "undefined") {
        // This is IE, which only fires keypress events for printable keys
        return true;
    } else if (typeof evt.which == "number" && evt.which > 0) {
        // In other browsers except old versions of WebKit, evt.which is
        // only greater than zero if the keypress is a printable key.
        // We need to filter out non printable chars and ctrl/alt/meta key combinations
        return !evt.ctrlKey && !evt.metaKey && !evt.altKey && evt.which > 32;
    }
    return false;
}

function enableSelect2Keyboard(dropDownId) {
    var dropDown = $('#' + dropDownId);
    var s2Obj = dropDown.data('select2');
    delete s2Obj.listeners.keypress;
    s2Obj.on('keypress', function (evt) {
      var key = evt.which;
      var KEYS = {
        BACKSPACE: 8,
        TAB: 9,
        ENTER: 13,
        SHIFT: 16,
        CTRL: 17,
        ALT: 18,
        ESC: 27,
        SPACE: 32,
        PAGE_UP: 33,
        PAGE_DOWN: 34,
        END: 35,
        HOME: 36,
        LEFT: 37,
        UP: 38,
        RIGHT: 39,
        DOWN: 40,
        DELETE: 46
      };
      if (this.isOpen()) {
        if (key === KEYS.ENTER) {
          this.trigger('results:select');
          evt.preventDefault();
        } else if ((key === KEYS.SPACE && evt.ctrlKey)) {
          this.trigger('results:toggle');
    
          evt.preventDefault();
        } else if (key === KEYS.UP) {
          this.trigger('results:previous');
    
          evt.preventDefault();
        } else if (key === KEYS.DOWN) {
          this.trigger('results:next');
    
          evt.preventDefault();
        } else if (key === KEYS.ESC || key === KEYS.TAB) {
          this.close();
    
          evt.preventDefault();
        }
      } else {
        if (key === KEYS.ENTER || key === KEYS.SPACE ||
            ((key === KEYS.DOWN || key === KEYS.UP) && evt.altKey)) {
          this.open();
    
          evt.preventDefault();
        } else if (key === KEYS.DOWN) {
            var val = s2Obj.$element.find('option:selected').next().val();
            if (undefined !== val) {
                s2Obj.$element.val(val);
                s2Obj.$element.trigger('change');
            } 
            evt.preventDefault();
        } else if (key === KEYS.UP) {
          if (undefined != this.$element.find('option:selected').prev().val()) {
            this.$element.val(this.$element.find('option:selected').prev().val());
            this.$element.trigger('change');
          }
          evt.preventDefault();
        } else if (isCharacterKeyPress(evt)) {
          // mimics typing in directly by matching the first letter typed (a browser collects
          // all typed chars and allows going beyond first letter)
          var option = this.$element.find('option').first();
          var theChar = String.fromCharCode(key).toLowerCase()
          while (undefined != option.val() && option.text().toLowerCase().indexOf(theChar) !== 0) {
            option = option.next()
          }
          if (undefined != option.val()) {
            this.$element.val(option.val());
            this.$element.trigger('change');
          }
          evt.preventDefault();
        }
       }
      }
     );
     
     s2Obj.on('close', function () {
         this.$element.trigger('focus');
     });
}