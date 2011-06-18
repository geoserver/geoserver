	//replace tabulation by the good number of white spaces
	EditArea.prototype.replace_tab= function(text){
		return text.replace(/((\n?)([^\t\n]*)\t)/gi, editArea.smartTab);		// slower than simple replace...	
	};
	
	// call by the replace_tab function
	EditArea.prototype.smartTab= function(){
		val="                   ";
		return EditArea.prototype.smartTab.arguments[2] + EditArea.prototype.smartTab.arguments[3] + val.substr(0, editArea.tab_nb_char - (EditArea.prototype.smartTab.arguments[3].length)%editArea.tab_nb_char);
	};
	
	EditArea.prototype.show_waiting_screen= function(){
		width= this.editor_area.offsetWidth;
		height= this.editor_area.offsetHeight;
		if(this.nav['isGecko'] || this.nav['isOpera'] || this.nav['isIE']>=7){
			width-=2;
			height-=2;
		}
		this.processing_screen.style.display="block";
		this.processing_screen.style.width= width+"px";
		this.processing_screen.style.height= height+"px";
		this.waiting_screen_displayed= true;
	};
	
	EditArea.prototype.hide_waiting_screen= function(){
		this.processing_screen.style.display="none";
		this.waiting_screen_displayed= false;
	};
	
	EditArea.prototype.add_style= function(styles){
		if(styles.length>0){
			newcss = document.createElement("style");
			newcss.type="text/css";
			newcss.media="all";
			if(newcss.styleSheet){ // IE
				newcss.styleSheet.cssText = styles;
			} else { // W3C
				newcss.appendChild(document.createTextNode(styles));
			}
			document.getElementsByTagName("head")[0].appendChild(newcss);
		}
	};
	
	EditArea.prototype.set_font= function(family, size){
		var elems= new Array("textarea", "content_highlight", "cursor_pos", "end_bracket", "selection_field", "line_number");
		if(family && family!="")
			this.settings["font_family"]= family;
		if(size && size>0)
			this.settings["font_size"]=size;
		if(this.nav['isOpera'])	// opera can't manage non monospace font
			this.settings['font_family']="monospace";
		var elem_font=$("area_font_size");	
		if(elem_font){	
			for(var i=0; i<elem_font.length; i++){
				if(elem_font.options[i].value && elem_font.options[i].value == this.settings["font_size"])
						elem_font.options[i].selected=true;
			}
		}
		
		// calc line height
		elem	= $("test_font_size");
		elem.style.fontFamily= ""+this.settings["font_family"];
		elem.style.fontSize= this.settings["font_size"]+"pt";				
		elem.innerHTML="0";		
		this.lineHeight= elem.offsetHeight;

		
		for(var i=0; i<elems.length; i++){
			var elem= $(elems[i]);	
			elem.style.fontFamily= this.settings["font_family"];
			elem.style.fontSize= this.settings["font_size"]+"pt";
			elem.style.lineHeight= this.lineHeight+"px";

		}
		if(this.nav['isOpera']){	// opera doesn't update font change to the textarea
			var start=this.textarea.selectionStart;
			var end= this.textarea.selectionEnd;
			var parNod = this.textarea.parentNode, nxtSib = this.textarea.nextSibling;
			parNod.removeChild(this.textarea); parNod.insertBefore(this.textarea, nxtSib);
			this.area_select(start, end-start);
		}
		
		this.add_style("pre{font-family:"+this.settings["font_family"]+"}");
		
		//alert(	getAttribute($("edit_area_test_font_size"), "style"));
		

		//alert("font "+this.textarea.style.font);
		// force update of selection field
		this.last_line_selected=-1;
		//if(this.state=="loaded"){
		this.last_selection= new Array();
		this.resync_highlight();
		//}
	/*	this.last_selection["indexOfCursor"]=-1;
		this.last_selection["curr_pos"]=-1;
		this.last_selection["line_start"]=-1;
		this.focus();*/
		//this.check_line_selection(false);
		//alert("line_h"+ this.lineHeight + " this.id: "+this.id+ "(size: "+size+")");
	};
	
	EditArea.prototype.change_font_size= function(){
		var size=$("area_font_size").value;
		if(size>0)
			this.set_font("", size);			
	};
	
	
	EditArea.prototype.open_inline_popup= function(popup_id){
		this.close_all_inline_popup();
		var popup= $(popup_id);		
		var editor= $("editor");
		
		// search matching icon
		for(var i=0; i<this.inlinePopup.length; i++){
			if(this.inlinePopup[i]["popup_id"]==popup_id){
				var icon= $(this.inlinePopup[i]["icon_id"]);
				if(icon){
					this.switchClassSticky(icon, 'editAreaButtonSelected', true);			
					break;
				}
			}
		}
		// check size
		popup.style.height="auto";
		popup.style.overflow= "visible";
			
		if(document.body.offsetHeight< popup.offsetHeight){
			popup.style.height= (document.body.offsetHeight-10)+"px";
			popup.style.overflow= "auto";
		}
		
		if(!popup.positionned){
			var new_left= editor.offsetWidth /2 - popup.offsetWidth /2;
			var new_top= editor.offsetHeight /2 - popup.offsetHeight /2;
			//var new_top= area.offsetHeight /2 - popup.offsetHeight /2;
			//var new_left= area.offsetWidth /2 - popup.offsetWidth /2;
			//alert("new_top: ("+new_top+") = calculeOffsetTop(area) ("+calculeOffsetTop(area)+") + area.offsetHeight /2("+ area.offsetHeight /2+") - popup.offsetHeight /2("+popup.offsetHeight /2+") - scrollTop: "+document.body.scrollTop);
			popup.style.left= new_left+"px";
			popup.style.top= new_top+"px";
			popup.positionned=true;
		}
		popup.style.visibility="visible";
		
		//popup.style.display="block";
	};

	EditArea.prototype.close_inline_popup= function(popup_id){
		var popup= $(popup_id);		
		// search matching icon
		for(var i=0; i<this.inlinePopup.length; i++){
			if(this.inlinePopup[i]["popup_id"]==popup_id){
				var icon= $(this.inlinePopup[i]["icon_id"]);
				if(icon){
					this.switchClassSticky(icon, 'editAreaButtonNormal', false);			
					break;
				}
			}
		}
		
		popup.style.visibility="hidden";	
	};
	
	EditArea.prototype.close_all_inline_popup= function(e){
		for(var i=0; i<this.inlinePopup.length; i++){
			this.close_inline_popup(this.inlinePopup[i]["popup_id"]);		
		}
		this.textarea.focus();
	};
	
	EditArea.prototype.show_help= function(){
		
		this.open_inline_popup("edit_area_help");
		
	};
			
	EditArea.prototype.new_document= function(){
		this.textarea.value="";
		this.area_select(0,0);
	};
	
	EditArea.prototype.get_all_toolbar_height= function(){
		var area= $("editor");
		var results= parent.getChildren(area, "div", "class", "area_toolbar", "all", "0");	// search only direct children
		//results= results.concat(getChildren(area, "table", "class", "area_toolbar", "all", "0"));
		var height=0;
		for(var i=0; i<results.length; i++){			
			height+= results[i].offsetHeight;
		}
		//alert("toolbar height: "+height);
		return height;
	};
	
	EditArea.prototype.go_to_line= function(line){	
		if(!line)
		{	
			var icon= $("go_to_line");
			if(icon != null){
				this.restoreClass(icon);
				this.switchClassSticky(icon, 'editAreaButtonSelected', true);
			}
			
			line= prompt(this.get_translation("go_to_line_prompt"));
			if(icon != null)
				this.switchClassSticky(icon, 'editAreaButtonNormal', false);
		}
		if(line && line!=null && line.search(/^[0-9]+$/)!=-1){
			var start=0;
			var lines= this.textarea.value.split("\n");
			if(line > lines.length)
				start= this.textarea.value.length;
			else{
				for(var i=0; i<Math.min(line-1, lines.length); i++)
					start+= lines[i].length + 1;
			}
			this.area_select(start, 0);
		}
		
		
	};
	
	
	EditArea.prototype.change_smooth_selection_mode= function(setTo){
		//alert("setTo: "+setTo);
		if(this.do_highlight)
			return;
			
		if(setTo != null){
			if(setTo === false)
				this.smooth_selection=true;
			else
				this.smooth_selection=false;
		}
		var icon= $("change_smooth_selection");
		this.textarea.focus();
		if(this.smooth_selection===true){
			//setAttribute(icon, "class", getAttribute(icon, "class").replace(/ selected/g, "") );
			/*setAttribute(icon, "oldClassName", "editAreaButtonNormal" );
			setAttribute(icon, "className", "editAreaButtonNormal" );*/
			//this.restoreClass(icon);
			//this.restoreAndSwitchClass(icon,'editAreaButtonNormal');
			this.switchClassSticky(icon, 'editAreaButtonNormal', false);
			
			this.smooth_selection=false;
			this.selection_field.style.display= "none";
			$("cursor_pos").style.display= "none";
			$("end_bracket").style.display= "none";
		}else{
			//setAttribute(icon, "class", getAttribute(icon, "class") + " selected");
			//this.switchClass(icon,'editAreaButtonSelected');
			this.switchClassSticky(icon, 'editAreaButtonSelected', false);
			this.smooth_selection=true;
			this.selection_field.style.display= "block";
			$("cursor_pos").style.display= "block";
			$("end_bracket").style.display= "block";
		}	
	};
	
	// the auto scroll of the textarea has some lacks when it have to show cursor in the visible area when the textarea size change
	// show specifiy whereas it is the "top" or "bottom" of the selection that is showned
	EditArea.prototype.scroll_to_view= function(show){
		if(!this.smooth_selection)
			return;
		var zone= $("result");
		
		//var cursor_pos_top= parseInt($("cursor_pos").style.top.replace("px",""));
		var cursor_pos_top= $("cursor_pos").cursor_top;
		if(show=="bottom")
			cursor_pos_top+= (this.last_selection["line_nb"]-1)* this.lineHeight;
			
		var max_height_visible= zone.clientHeight + zone.scrollTop;
		var miss_top= cursor_pos_top + this.lineHeight - max_height_visible;
		if(miss_top>0){
			//alert(miss_top);
			zone.scrollTop=  zone.scrollTop + miss_top;
		}else if( zone.scrollTop > cursor_pos_top){
			// when erase all the content -> does'nt scroll back to the top
			//alert("else: "+cursor_pos_top);
			zone.scrollTop= cursor_pos_top;	 
		}
		//var cursor_pos_left= parseInt($("cursor_pos").style.left.replace("px",""));
		var cursor_pos_left= $("cursor_pos").cursor_left;
		var max_width_visible= zone.clientWidth + zone.scrollLeft;
		var miss_left= cursor_pos_left + 10 - max_width_visible;
		if(miss_left>0){			
			zone.scrollLeft= zone.scrollLeft + miss_left + 50;
		}else if( zone.scrollLeft > cursor_pos_left){
			zone.scrollLeft= cursor_pos_left ;
		}else if( zone.scrollLeft == 45){
			// show the line numbers if textarea align to it's left
			zone.scrollLeft=0;
		}
	};
	
	EditArea.prototype.check_undo= function(only_once){
		if(!editAreas[this.id])
			return false;
		if(this.textareaFocused && editAreas[this.id]["displayed"]==true){
			var text=this.textarea.value;
			if(this.previous.length<=1)
				this.switchClassSticky($("undo"), 'editAreaButtonDisabled', true);
		
			if(!this.previous[this.previous.length-1] || this.previous[this.previous.length-1]["text"] != text){
				this.previous.push({"text": text, "selStart": this.textarea.selectionStart, "selEnd": this.textarea.selectionEnd});
				if(this.previous.length > this.settings["max_undo"]+1)
					this.previous.shift();
				
			}
			if(this.previous.length >= 2)
				this.switchClassSticky($("undo"), 'editAreaButtonNormal', false);		
		}

		if(!only_once)
			setTimeout("editArea.check_undo()", 3000);
	};
	
	EditArea.prototype.undo= function(){
		//alert("undo"+this.previous.length);
		if(this.previous.length > 0){
			if(this.nav['isIE'])
				this.getIESelection();
		//	var pos_cursor=this.textarea.selectionStart;
			this.next.push({"text": this.textarea.value, "selStart": this.textarea.selectionStart, "selEnd": this.textarea.selectionEnd});
			var prev= this.previous.pop();
			if(prev["text"]==this.textarea.value && this.previous.length > 0)
				prev=this.previous.pop();						
			this.textarea.value= prev["text"];
			this.last_undo= prev["text"];
			this.area_select(prev["selStart"], prev["selEnd"]-prev["selStart"]);
			this.switchClassSticky($("redo"), 'editAreaButtonNormal', false);
			this.resync_highlight(true);
			//alert("undo"+this.previous.length);
			this.check_file_changes();
		}
	};
	
	EditArea.prototype.redo= function(){
		if(this.next.length > 0){
			/*if(this.nav['isIE'])
				this.getIESelection();*/
			//var pos_cursor=this.textarea.selectionStart;
			var next= this.next.pop();
			this.previous.push(next);
			this.textarea.value= next["text"];
			this.last_undo= next["text"];
			this.area_select(next["selStart"], next["selEnd"]-next["selStart"]);
			this.switchClassSticky($("undo"), 'editAreaButtonNormal', false);
			this.resync_highlight(true);
			this.check_file_changes();
		}
		if(	this.next.length == 0)
			this.switchClassSticky($("redo"), 'editAreaButtonDisabled', true);
	};
	
	EditArea.prototype.check_redo= function(){
		if(editArea.next.length == 0 || editArea.textarea.value!=editArea.last_undo){
			editArea.next= new Array();	// undo the ability to use "redo" button
			editArea.switchClassSticky($("redo"), 'editAreaButtonDisabled', true);
		}
		else
		{
			this.switchClassSticky($("redo"), 'editAreaButtonNormal', false);
		}
	};
	
	
	// functions that manage icons roll over, disabled, etc...
	EditArea.prototype.switchClass = function(element, class_name, lock_state) {
		var lockChanged = false;
	
		if (typeof(lock_state) != "undefined" && element != null) {
			element.classLock = lock_state;
			lockChanged = true;
		}
	
		if (element != null && (lockChanged || !element.classLock)) {
			element.oldClassName = element.className;
			element.className = class_name;
		}
	};
	
	EditArea.prototype.restoreAndSwitchClass = function(element, class_name) {
		if (element != null && !element.classLock) {
			this.restoreClass(element);
			this.switchClass(element, class_name);
		}
	};
	
	EditArea.prototype.restoreClass = function(element) {
		if (element != null && element.oldClassName && !element.classLock) {
			element.className = element.oldClassName;
			element.oldClassName = null;
		}
	};
	
	EditArea.prototype.setClassLock = function(element, lock_state) {
		if (element != null)
			element.classLock = lock_state;
	};
	
	EditArea.prototype.switchClassSticky = function(element, class_name, lock_state) {
		var lockChanged = false;
		if (typeof(lock_state) != "undefined" && element != null) {
			element.classLock = lock_state;
			lockChanged = true;
		}
	
		if (element != null && (lockChanged || !element.classLock)) {
			element.className = class_name;
			element.oldClassName = class_name;
		}
	};
	
	//make the "page up" and "page down" buttons works correctly
	EditArea.prototype.scroll_page= function(params){
		var dir= params["dir"];
		var shift_pressed= params["shift"];
		screen_height=$("result").clientHeight;
		var lines= this.textarea.value.split("\n");		
		var new_pos=0;
		var length=0;
		var char_left=0;
		var line_nb=0;
		if(dir=="up"){
			//val= Math.max(0, $("result").scrollTop - screen_height);
			//$("result").scrollTop= val;
			var scroll_line= Math.ceil((screen_height -30)/this.lineHeight);
			if(this.last_selection["selec_direction"]=="up"){
				for(line_nb=0; line_nb< Math.min(this.last_selection["line_start"]-scroll_line, lines.length); line_nb++){
					new_pos+= lines[line_nb].length + 1;
				}
				char_left=Math.min(lines[Math.min(lines.length-1, line_nb)].length, this.last_selection["curr_pos"]-1);
				if(shift_pressed)
					length=this.last_selection["selectionEnd"]-new_pos-char_left;	
				this.area_select(new_pos+char_left, length);
				view="top";
			}else{			
				view="bottom";
				for(line_nb=0; line_nb< Math.min(this.last_selection["line_start"]+this.last_selection["line_nb"]-1-scroll_line, lines.length); line_nb++){
					new_pos+= lines[line_nb].length + 1;
				}
				char_left=Math.min(lines[Math.min(lines.length-1, line_nb)].length, this.last_selection["curr_pos"]-1);
				if(shift_pressed){
					//length=this.last_selection["selectionEnd"]-new_pos-char_left;	
					start= Math.min(this.last_selection["selectionStart"], new_pos+char_left);
					length= Math.max(new_pos+char_left, this.last_selection["selectionStart"] )- start ;
					if(new_pos+char_left < this.last_selection["selectionStart"])
						view="top";
				}else
					start=new_pos+char_left;
				this.area_select(start, length);
				
			}
		}else{
			//val= Math.max($("result").style.height.replace("px", ""), $("result").scrollTop + screen_height);
			//$("result").scrollTop= val;
			var scroll_line= Math.floor((screen_height-30)/this.lineHeight);				
			if(this.last_selection["selec_direction"]=="down"){
				view="bottom";
				for(line_nb=0; line_nb< Math.min(this.last_selection["line_start"]+this.last_selection["line_nb"]-2+scroll_line, lines.length); line_nb++){
					if(line_nb==this.last_selection["line_start"]-1)
						char_left= this.last_selection["selectionStart"] -new_pos;
					new_pos+= lines[line_nb].length + 1;
									
				}
				if(shift_pressed){
					length=Math.abs(this.last_selection["selectionStart"]-new_pos);	
					length+=Math.min(lines[Math.min(lines.length-1, line_nb)].length, this.last_selection["curr_pos"]);
					//length+=Math.min(lines[Math.min(lines.length-1, line_nb)].length, char_left);
					this.area_select(Math.min(this.last_selection["selectionStart"], new_pos), length);
				}else{
					this.area_select(new_pos+char_left, 0);
				}
				
			}else{
				view="top";
				for(line_nb=0; line_nb< Math.min(this.last_selection["line_start"]+scroll_line-1, lines.length, lines.length); line_nb++){
					if(line_nb==this.last_selection["line_start"]-1)
						char_left= this.last_selection["selectionStart"] -new_pos;
					new_pos+= lines[line_nb].length + 1;									
				}
				if(shift_pressed){
					length=Math.abs(this.last_selection["selectionEnd"]-new_pos-char_left);	
					length+=Math.min(lines[Math.min(lines.length-1, line_nb)].length, this.last_selection["curr_pos"])- char_left-1;
					//length+=Math.min(lines[Math.min(lines.length-1, line_nb)].length, char_left);
					this.area_select(Math.min(this.last_selection["selectionEnd"], new_pos+char_left), length);
					if(new_pos+char_left > this.last_selection["selectionEnd"])
						view="bottom";
				}else{
					this.area_select(new_pos+char_left, 0);
				}
				
			}
		}		
		
		this.check_line_selection();
		this.scroll_to_view(view);
	};
	
	EditArea.prototype.start_resize= function(e){		
		parent.editAreaLoader.resize["id"]= editArea.id;		
		parent.editAreaLoader.resize["start_x"]= (e)? e.pageX : event.x + document.body.scrollLeft;		
		parent.editAreaLoader.resize["start_y"]= (e)? e.pageY : event.y + document.body.scrollTop;
		if(editArea.nav['isIE']){
			editArea.textarea.focus();
			editArea.getIESelection();
		}
		parent.editAreaLoader.resize["selectionStart"]= editArea.textarea.selectionStart;
		parent.editAreaLoader.resize["selectionEnd"]= editArea.textarea.selectionEnd;
		/*parent.editAreaLoader.resize["frame_top"]= parent.calculeOffsetTop(parent.editAreas[editArea.id]["textarea"]);
		/*parent.editAreaLoader.resize["frame_left"]= parent.calculeOffsetLeft(parent.frames[editArea.id]);*/
		parent.editAreaLoader.start_resize_area();
	};
	
	EditArea.prototype.toggle_full_screen= function(to){
		if(typeof(to)=="undefined")
			to= !this.fullscreen['isFull'];
		var old= this.fullscreen['isFull'];
		this.fullscreen['isFull']= to;
		var icon= $("fullscreen");
		if(to && to!=old)
		{	// toogle on fullscreen		
			var selStart= this.textarea.selectionStart;
			var selEnd= this.textarea.selectionEnd;
			var html= parent.document.getElementsByTagName("html")[0];
			var frame= parent.document.getElementById("frame_"+this.id);

			this.fullscreen['old_overflow']= parent.get_css_property(html, "overflow");
			this.fullscreen['old_height']= parent.get_css_property(html, "height");
			this.fullscreen['old_width']= parent.get_css_property(html, "width");
			this.fullscreen['old_scrollTop']= html.scrollTop;
			this.fullscreen['old_scrollLeft']= html.scrollLeft;
			this.fullscreen['old_zIndex']= parent.get_css_property(frame, "z-index");
			if(this.nav['isOpera']){
				html.style.height= "100%";
				html.style.width= "100%";	
			}
			html.style.overflow= "hidden";
			html.scrollTop=0;
			html.scrollLeft=0;
			
		
			//html.style.backgroundColor= "#FF0000"; 
//	alert(screen.height+"\n"+window.innerHeight+"\n"+html.clientHeight+"\n"+window.offsetHeight+"\n"+document.body.offsetHeight);
			
			
			frame.style.position="absolute";
			frame.style.width= html.clientWidth+"px";
			frame.style.height= html.clientHeight+"px";
			frame.style.display="block";
			frame.style.zIndex="999999";
			frame.style.top="0px";
			frame.style.left="0px";
			
			// if the iframe was in a div with position absolute, the top and left are the one of the div, 
			// so I fix it by seeing at witch position the iframe start and correcting it
			frame.style.top= "-"+parent.calculeOffsetTop(frame)+"px";
			frame.style.left= "-"+parent.calculeOffsetLeft(frame)+"px";
			
		//	parent.editAreaLoader.execCommand(this.id, "update_size();");
		//	var body=parent.document.getElementsByTagName("body")[0];
		//	body.appendChild(frame);
			
			this.switchClassSticky(icon, 'editAreaButtonSelected', false);
			this.fullscreen['allow_resize']= this.resize_allowed;
			this.allow_resize(false);
	
			//this.area_select(selStart, selEnd-selStart);
			
		
			// opera can't manage to do a direct size update
			if(this.nav['isFirefox']){
				parent.editAreaLoader.execCommand(this.id, "update_size();");
				this.area_select(selStart, selEnd-selStart);
				this.scroll_to_view();
				this.focus();
			}else{
				setTimeout("parent.editAreaLoader.execCommand('"+ this.id +"', 'update_size();');editArea.focus();", 10);
			}	
			
	
		}
		else if(to!=old)
		{	// toogle off fullscreen
			var selStart= this.textarea.selectionStart;
			var selEnd= this.textarea.selectionEnd;
			
			var frame= parent.document.getElementById("frame_"+this.id);	
			frame.style.position="static";
			frame.style.zIndex= this.fullscreen['old_zIndex'];
		
			var html= parent.document.getElementsByTagName("html")[0];
		//	html.style.overflow= this.fullscreen['old_overflow'];
		
			if(this.nav['isOpera']){
				html.style.height= "auto"; 
				html.style.width= "auto";
				html.style.overflow= "auto";
			}else if(this.nav['isIE'] && parent!=top){	// IE doesn't manage html overflow in frames like in normal page... 
				html.style.overflow= "auto";
			}
			else
				html.style.overflow= this.fullscreen['old_overflow'];
			html.scrollTop= this.fullscreen['old_scrollTop'];
			html.scrollTop= this.fullscreen['old_scrollLeft'];
		
			parent.editAreaLoader.hide(this.id);
			parent.editAreaLoader.show(this.id);
			
			this.switchClassSticky(icon, 'editAreaButtonNormal', false);
			if(this.fullscreen['allow_resize'])
				this.allow_resize(this.fullscreen['allow_resize']);
			if(this.nav['isFirefox']){
				this.area_select(selStart, selEnd-selStart);
				setTimeout("editArea.scroll_to_view();", 10);
			}			
			
			//parent.editAreaLoader.remove_event(parent.window, "resize", editArea.update_size);
		}
		
	};
	
	EditArea.prototype.allow_resize= function(allow){
		var resize= $("resize_area");
		if(allow){
			
			resize.style.visibility="visible";
			parent.editAreaLoader.add_event(resize, "mouseup", editArea.start_resize);
		}else{
			resize.style.visibility="hidden";
			parent.editAreaLoader.remove_event(resize, "mouseup", editArea.start_resize);
		}
		this.resize_allowed= allow;
	};
	
	
	EditArea.prototype.change_syntax= function(new_syntax, is_waiting){
	//	alert("cahnge to "+new_syntax);
		// the syntax is the same
		if(new_syntax==this.settings['syntax'])
			return true;
		
		// check that the syntax is one allowed
		var founded= false;
		for(var i=0; i<this.syntax_list.length; i++)
		{
			if(this.syntax_list[i]==new_syntax)
				founded= true;
		}
		
		if(founded==true)
		{
			// the reg syntax file is not loaded
			if(!parent.editAreaLoader.load_syntax[new_syntax])
			{
				// load the syntax file and wait for file loading
				if(!is_waiting)
					parent.editAreaLoader.load_script(parent.editAreaLoader.baseURL + "reg_syntax/" + new_syntax + ".js");
				setTimeout("editArea.change_syntax('"+ new_syntax +"', true);", 100);
				this.show_waiting_screen();
			}
			else
			{
				if(!this.allready_used_syntax[new_syntax])
				{	// the syntax has still not been used
					// rebuild syntax definition for new languages
					parent.editAreaLoader.init_syntax_regexp();
					// add style to the new list
					this.add_style(parent.editAreaLoader.syntax[new_syntax]["styles"]);
					this.allready_used_syntax[new_syntax]=true;
				}
				// be sure that the select option is correctly updated
				var sel= $("syntax_selection");
				if(sel && sel.value!=new_syntax)
				{
					for(var i=0; i<sel.length; i++){
						if(sel.options[i].value && sel.options[i].value == new_syntax)
							sel.options[i].selected=true;
					}
				}
				
			/*	if(this.settings['syntax'].length==0)
				{
					this.switchClassSticky($("highlight"), 'editAreaButtonNormal', false);
					this.switchClassSticky($("reset_highlight"), 'editAreaButtonNormal', false);
					this.change_highlight(true);
				}
				*/
				this.settings['syntax']= new_syntax;
				this.resync_highlight(true);
				this.hide_waiting_screen();
				return true;
			}
		}
		return false;
	};
	
	
	// check if the file has changed
	EditArea.prototype.set_editable= function(is_editable){
		if(is_editable)
		{
			document.body.className= "";
			this.textarea.readOnly= false;
			this.is_editable= true;
		}
		else
		{
			document.body.className= "non_editable";
			this.textarea.readOnly= true;
			this.is_editable= false;
		}
		
		if(editAreas[this.id]["displayed"]==true)
			this.update_size();
	};
	
	/***** Wrap mode *****/
	// open a new tab for the given file
	EditArea.prototype.set_wrap_text= function(to){
		this.settings['wrap_text']	= to;
		if( this.settings['wrap_text'] )
		{
			wrap_mode = 'soft';
			this.container.className+= ' wrap_text';
		}
		else
		{
			wrap_mode = 'off';
			this.container.className= this.container.className.replace(/ wrap_text/g, '');
		}
		
		
		var t= this.textarea;
		t.wrap= wrap_mode;
		t.setAttribute('wrap', wrap_mode);
		// seul IE supporte de changer à la volée le wrap mode du textarea
		if(!this.nav['isIE']){
			var start=t.selectionStart, end= t.selectionEnd;
			var parNod = t.parentNode, nxtSib = t.nextSibling;
			parNod.removeChild(t); parNod.insertBefore(t, nxtSib);
			this.area_select(start, end-start);
	/*	//	v = s.value;
			n = s.cloneNode(true);
			n.setAttribute("wrap", val);
			s.parentNode.replaceChild(n, s);
		//	n.value = v;*/
		}
	};	
	/***** tabbed files managing functions *****/
	
	// open a new tab for the given file
	EditArea.prototype.open_file= function(settings){
		
		if(settings['id']!="undefined")
		{
			var id= settings['id'];
			// create a new file object with defautl values
			var new_file= new Object();
			new_file['id']= id;
			new_file['title']= id;
			new_file['text']= "";
			new_file['last_selection']= "";		
			new_file['last_text_to_highlight']= "";
			new_file['last_hightlighted_text']= "";
			new_file['previous']= new Array();
			new_file['next']= new Array();
			new_file['last_undo']= "";
			new_file['smooth_selection']= this.settings['smooth_selection'];
			new_file['do_highlight']= this.settings['start_highlight'];
			new_file['syntax']= this.settings['syntax'];
			new_file['scroll_top']= 0;
			new_file['scroll_left']= 0;
			new_file['selection_start']= 0;
			new_file['selection_end']= 0;
			new_file['edited']= false;
			new_file['font_size']= this.settings["font_size"];
			new_file['font_family']= this.settings["font_family"];
			new_file['toolbar']= {'links':{}, 'selects': {}};
			new_file['compare_edited_text']= new_file['text'];
			
			
			this.files[id]= new_file;
			this.update_file(id, settings);
			this.files[id]['compare_edited_text']= this.files[id]['text'];
			
			
			var html_id= 'tab_file_'+encodeURIComponent(id);
			this.filesIdAssoc[html_id]= id;
			this.files[id]['html_id']= html_id;
		
			if(!$(this.files[id]['html_id']) && id!="")
			{
				// be sure the tab browsing area is displayed
				this.tab_browsing_area.style.display= "block";
				var elem= document.createElement('li');
				elem.id= this.files[id]['html_id'];
				var close= "<img src=\""+ parent.editAreaLoader.baseURL +"images/close.gif\" title=\""+ this.get_translation('close_tab', 'word') +"\" onclick=\"editArea.execCommand('close_file', editArea.filesIdAssoc['"+ html_id +"']);return false;\" class=\"hidden\" onmouseover=\"this.className=''\" onmouseout=\"this.className='hidden'\" />";
				elem.innerHTML= "<a onclick=\"javascript:editArea.execCommand('switch_to_file', editArea.filesIdAssoc['"+ html_id +"']);\" selec=\"none\"><b><span><strong class=\"edited\">*</strong>"+ this.files[id]['title'] + close +"</span></b></a>";
				$('tab_browsing_list').appendChild(elem);
				var elem= document.createElement('text');
				this.update_size();
			}
			
			// open file callback (for plugin)
			if(id!="")
				this.execCommand('file_open', this.files[id]);
			
			this.switch_to_file(id, true);
			return true;
		}
		else
			return false;
	};
	
	// close the given file
	EditArea.prototype.close_file= function(id){
		if(this.files[id])
		{
			this.save_file(id);
			
			// close file callback
			if(this.execCommand('file_close', this.files[id])!==false)
			{
				// remove the tab in the toolbar
				var li= $(this.files[id]['html_id']);
				li.parentNode.removeChild(li);
				// select a new file
				if(id== this.curr_file)
				{
					var next_file= "";
					var is_next= false;
					for(var i in this.files)
					{
						if(is_next)
						{
							next_file= i;
							break;
						}
						else if(i==id)
							is_next= true;
						else
							next_file= i;
					}
					// display the next file
					this.switch_to_file(next_file);
				}
				// clear datas
				delete (this.files[id]);
				this.update_size();
			}	
		}
	};
	
	// backup current file datas
	EditArea.prototype.save_file= function(id){
		if(this.files[id])
		{
			var save= this.files[id];
			save['last_selection']= this.last_selection;		
			save['last_text_to_highlight']= this.last_text_to_highlight;
			save['last_hightlighted_text']= this.last_hightlighted_text;
			save['previous']= this.previous;
			save['next']= this.next;
			save['last_undo']= this.last_undo;
			save['smooth_selection']= this.smooth_selection;
			save['do_highlight']= this.do_highlight;
			save['syntax']= this.settings['syntax'];
			save['text']= this.textarea.value;
			save['scroll_top']= this.result.scrollTop;
			save['scroll_left']= this.result.scrollLeft;
			save['selection_start']= this.last_selection["selectionStart"];
			save['selection_end']= this.last_selection["selectionEnd"];
			save['font_size']= this.settings["font_size"];
			save['font_family']= this.settings["font_family"];
			save['toolbar']= {'links':{}, 'selects': {}};
			// save toolbar buttons state for fileSpecific buttons
			var links= $("toolbar_1").getElementsByTagName("a");
			for(var i=0; i<links.length; i++)
			{
				if(links[i].getAttribute('fileSpecific')=='yes')
				{
					var save_butt= new Object();
					var img= links[i].getElementsByTagName('img')[0];
					save_butt['classLock']= img.classLock;
					save_butt['className']= img.className;
					save_butt['oldClassName']= img.oldClassName;
					
					save['toolbar']['links'][links[i].id]= save_butt;
				}
			}
			// save toolbar select state for fileSpecific buttons
			var selects= $("toolbar_1").getElementsByTagName("select");
			for(var i=0; i<selects.length; i++)
			{
				if(selects[i].getAttribute('fileSpecific')=='yes')
				{
					save['toolbar']['selects'][selects[i].id]= selects[i].value;
				}
			}
				
			this.files[id]= save;
			
			return save;
		}
		else
			return false;
	};
	
	// update file_datas
	EditArea.prototype.update_file= function(id, new_values){
		for(var i in new_values)
		{
			this.files[id][i]= new_values[i];
		}
	};
	
	// display file datas
	EditArea.prototype.display_file= function(id){
		// check if there is at least one tab file displayed
		if(id=='')
		{
			this.textarea.readOnly= true;
			this.tab_browsing_area.style.display= "none";
			$("no_file_selected").style.display= "block";
			this.result.className= "empty";
			if(!this.files[''])
				this.open_file({id: ''});
		}
		else
		{
			this.result.className= "";
			this.textarea.readOnly= !this.is_editable;
			$("no_file_selected").style.display= "none";
			this.tab_browsing_area.style.display= "block";
		}
		
		this.check_redo(true);
		this.check_undo(true);
		this.curr_file= id;
		
		// replace selected tab file
		var lis= this.tab_browsing_area.getElementsByTagName('li');
		for(var i=0; i<lis.length; i++)
		{
			if(lis[i].id == this.files[id]['html_id'])
				lis[i].className='selected';
			else
				lis[i].className='';
		}
		
		// replace next files datas
		var new_file= this.files[id];
	
		// restore text content
		this.textarea.value= new_file['text'];
		
		// restore font-size
		this.set_font(new_file['font_family'], new_file['font_size']);
		
		// restore selection and scroll
		this.area_select(new_file['last_selection']['selection_start'], new_file['last_selection']['selection_end'] - new_file['last_selection']['selection_start']);
		this.manage_size(true);
		this.result.scrollTop= new_file['scroll_top'];
		this.result.scrollLeft= new_file['scroll_left'];
		
		// restore undo, redo
		this.previous=	new_file['previous'];
		this.next=	new_file['next'];
		this.last_undo=	new_file['last_undo'];
		this.check_redo(true);
		this.check_undo(true);
		
		// restore highlight
		this.execCommand("change_highlight", new_file['do_highlight']);
		this.execCommand("change_syntax", new_file['syntax']);
		
		// smooth mode
		this.execCommand("change_smooth_selection_mode", new_file['smooth_selection']);
			
		// restore links state in toolbar
		var links= new_file['toolbar']['links'];
		for(var i in links)
		{
			if(img= $(i).getElementsByTagName('img')[0])
			{
				var save_butt= new Object();
				img.classLock= links[i]['classLock'];
				img.className= links[i]['className'];
				img.oldClassName= links[i]['oldClassName'];
			}
		}
		// restore select state in toolbar
		var selects= new_file['toolbar']['selects'];
		for(var i in selects)
		{
			var options= $(i).options;
			for(var j=0; j<options.length; j++)
			{
				if(options[j].value == selects[i])
					$(i).options[j].selected=true;
			}
		}
	
	};

	// change tab for displaying a new one
	EditArea.prototype.switch_to_file= function(file_to_show, force_refresh){
		if(file_to_show!=this.curr_file || force_refresh)
		{
			this.save_file(this.curr_file);
			if(this.curr_file!='')
				this.execCommand('file_switch_off', this.files[this.curr_file]);
			this.display_file(file_to_show);
			if(file_to_show!='')
				this.execCommand('file_switch_on', this.files[file_to_show]);
		}
	};

	// get all infos for the given file
	EditArea.prototype.get_file= function(id){
		if(id==this.curr_file)
			this.save_file(id);
		return this.files[id];
	};
	
	// get all available files infos
	EditArea.prototype.get_all_files= function(){
		tmp_files= this.files;
		this.save_file(this.curr_file);
		if(tmp_files[''])
			delete(this.files['']);
		return tmp_files;
	};
	
	
	// check if the file has changed
	EditArea.prototype.check_file_changes= function(){
	
		var id= this.curr_file;
		if(this.files[id] && this.files[id]['compare_edited_text']!=undefined)
		{
			if(this.files[id]['compare_edited_text'].length==this.textarea.value.length && this.files[id]['compare_edited_text']==this.textarea.value)
			{
				if(this.files[id]['edited']!= false)
					this.set_file_edited_mode(id, false);
			}
			else
			{
				if(this.files[id]['edited']!= true)
					this.set_file_edited_mode(id, true);
			}
		}
	};
	
	// set if the file is edited or not
	EditArea.prototype.set_file_edited_mode= function(id, to){
		// change CSS for edited tab
		if(this.files[id] && $(this.files[id]['html_id']))
		{
			var link= $(this.files[id]['html_id']).getElementsByTagName('a')[0];
			if(to==true)
			{
				link.className= 'edited';
			}
			else
			{
				link.className= '';
				if(id==this.curr_file)
					text= this.textarea.value;
				else
					text= this.files[id]['text'];
				this.files[id]['compare_edited_text']= text;
			}
				
			this.files[id]['edited']= to;
		}
	};

	EditArea.prototype.set_show_line_colors = function(new_value){
		this.show_line_colors = new_value;
		
		if( new_value )
			this.selection_field.className	+= ' show_colors';
		else
			this.selection_field.className	= this.selection_field.className.replace( / show_colors/g, '' );
	};