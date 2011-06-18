/**
 * Inline Info
 * A jQuery plugin allowing 'info' text to be placed dynamically for form inputs
 * (including password fields)
 * 
 * Version 0.5
 * Author: Chris Patterson
 *
 * License: GPL 3 http://www.gnu.org/licenses/gpl-3.0.html
 * 
 **/
(function($){
  $.fn.inlineInfoTextify = function(options) {
    
  var defaults = {
	 inlineInfoClass: "overlabel",
   inlineInfoText: "", /* if not passed in, fall through to using the title for this element, then to the relevant label, if possible */
   caseSensitiveMatch: "false" /* "true" primarily for password inputs, I expect */
  };
  
  var options = $.extend(defaults, options);

  var removeLabel = function(target) {
	  target.removeClass(options.inlineInfoClass).val("");
	}
	
	var addLabel = function(target, replaceWithText) {
	  target.addClass(options.inlineInfoClass).val(replaceWithText);
	}
	
  var replaceField = function(target, replaceWithType, replaceWithText, replacementText) {
		if ((target.val() == "") || (options.caseSensitiveMatch =="true" && target.val() == replacementText) || (target.val().toLowerCase() == replacementText.toLowerCase())) {

		  var replacementField = document.createElement('input');
		
			replacementField.type = replaceWithType;
		  if(target.attr('size')) replacementField.size = target.attr('size');
		  if(target.attr('value')) replacementField.value = replaceWithText;
		  if(target.attr('title')) replacementField.title = target.attr('title');
		  if(target.attr('maxlength')) replacementField.maxLength = target.attr('maxlength');
		  if(target.attr('name')) replacementField.name = target.attr('name');
		  if(target.attr('id')) replacementField.id = target.attr('id');
		  if(target.attr('class')) replacementField.className = target.attr('class');
		  replacementField.value = replaceWithText;
		
		  if (replaceWithType == 'text') {
			  $(replacementField).addClass(options.inlineInfoClass).focus(function(event) {
					replaceField($(this), 'password', '', replacementText);
		    });
		   target.replaceWith(replacementField);	
			} else { // We can't use replaceWith here, as we need to be able to set focus to the new element as part of the replace process
			  $(replacementField).removeClass(options.inlineInfoClass).blur(function(event) {
					replaceField($(this), 'text', replacementText, replacementText); 
		    }).insertAfter(target).focus();
		   target.remove();
			}
		}
	}

  return this.each(function() {
		obj = $(this);
		
		var replacementText = (options.inlineInfoText == "") ? ((obj.attr('title')) ? obj.attr('title') : $("label[for=" + obj.attr('id') +"]").text()) : options.inlineInfoText;

		if (obj.attr('type') == 'password') {/* password element */
			
		  replaceField(obj, 'text', replacementText, replacementText); // initialize field
		
			obj.focus(function(event) {
				replaceField(obj, 'password', '', replacementText); 
	    });
		
		} else { /* normal input */
			
			addLabel(obj, replacementText); // initialize field
			
			obj.focus(function(event) {
				var target = $(this);
	      if ((target.val() == "") || (options.caseSensitiveMatch =="true" && target.val() == replacementText) || (target.val().toLowerCase() == replacementText.toLowerCase())) {
	        removeLabel(target);
	      }
	    }).blur(function(event){
				var target = $(this);
	      if ((target.val() == "") || (options.caseSensitiveMatch =="true" && target.val() == replacementText) || (target.val().toLowerCase() == replacementText.toLowerCase())) {
	        addLabel(target, replacementText);
	      }
	    });
		}
  });
  };
})(jQuery);
