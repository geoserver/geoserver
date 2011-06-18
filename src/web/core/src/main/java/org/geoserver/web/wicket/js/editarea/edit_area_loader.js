/******
 *
 *	EditArea 
 * 	Developped by Christophe Dolivet
 *	Released under LGPL, Apache and BSD licenses (use the one you want)
 *
******/

function EditAreaLoader(){
	this.version= "0.7.3";
	date= new Date();
	this.start_time=date.getTime();
	this.win= "loading";	// window loading state
	this.error= false;	// to know if load is interrrupt
	this.baseURL="";
	//this.suffix="";
	this.template="";
	this.lang= new Object();	// array of loaded speech language
	this.load_syntax= new Object();	// array of loaded syntax language for highlight mode
	this.syntax= new Object();	// array of initilized syntax language for highlight mode
	this.loadedFiles= new Array();
	this.waiting_loading= new Object(); 	// files that must be loaded in order to allow the script to really start
	// scripts that must be loaded in the iframe
	this.scripts_to_load= new Array("elements_functions", "resize_area", "reg_syntax");
	this.sub_scripts_to_load= new Array("edit_area", "manage_area" ,"edit_area_functions", "keyboard", "search_replace", "highlight", "regexp" );
	
	this.resize= new Array(); // contain resizing datas
	this.hidden= new Object();	// store datas of the hidden textareas
	
	this.default_settings= {
		//id: "src"	// id of the textarea to transform
		debug: false
		,smooth_selection: true
		,font_size: "10"		// not for IE
		,font_family: "monospace"	// can be "verdana,monospace". Allow non monospace font but Firefox get smaller tabulation with non monospace fonts. IE doesn't change the tabulation width and Opera doesn't take this option into account... 
		,start_highlight: false	// if start with highlight
		,autocompletion: false	// DEV ONLY if enable auto completion			
		,toolbar: "search, go_to_line, fullscreen, |, undo, redo, |, select_font,|, change_smooth_selection, highlight, reset_highlight, |, help"
		,begin_toolbar: ""		//  "new_document, save, load, |"
		,end_toolbar: ""		// or end_toolbar
		,is_multi_files: false		// enable the multi file mode (the textarea content is ignored)
		,allow_resize: "both"	// possible values: "no", "both", "x", "y"
		,show_line_colors: false	// if the highlight is disabled for the line currently beeing edited (if enabled => heavy CPU use)
		,min_width: 400
		,min_height: 125
		,replace_tab_by_spaces: false
		,allow_toggle: true		// true or false
		,language: "en"
		,syntax: ""
		,syntax_selection_allow: "basic,brainfuck,c,coldfusion,cpp,css,html,js,pas,perl,php,python,ruby,robotstxt,sql,tsql,vb,xml"
		,display: "onload" 		// onload or later
		,max_undo: 30
		,browsers: "known"	// all or known
		,plugins: "" // comma separated plugin list
		,gecko_spellcheck: false	// enable/disable by default the gecko_spellcheck
		,fullscreen: false
		,is_editable: true
		,cursor_position: "begin"
		,wrap_text: false		// NOT IMPLEMENTED
		,load_callback: ""		// click on load button (function name)
		,save_callback: ""		// click on save button (function name)
		,change_callback: ""	// textarea onchange trigger (function name)
		,submit_callback: ""	// form submited (function name)
		,EA_init_callback: ""	// EditArea initiliazed (function name)
		,EA_delete_callback: ""	// EditArea deleted (function name)
		,EA_load_callback: ""	// EditArea fully loaded and displayed (function name)
		,EA_unload_callback: ""	// EditArea delete while being displayed (function name)
		,EA_toggle_on_callback: ""	// EditArea toggled on (function name)
		,EA_toggle_off_callback: ""	// EditArea toggled off (function name)
		,EA_file_switch_on_callback: ""	// a new tab is selected (called for the newly selected file)
		,EA_file_switch_off_callback: ""	// a new tab is selected (called for the previously selected file)
		,EA_file_close_callback: ""		// close a tab
	};
	
	this.advanced_buttons = [
			// id, button img, command (it will try to find the translation of "id"), is_file_specific
			['new_document', 'newdocument.gif', 'new_document', false],
			['search', 'search.gif', 'show_search', false],
			['go_to_line', 'go_to_line.gif', 'go_to_line', false],
			['undo', 'undo.gif', 'undo', true],
			['redo', 'redo.gif', 'redo', true],
			['change_smooth_selection', 'smooth_selection.gif', 'change_smooth_selection_mode', true],
			['reset_highlight', 'reset_highlight.gif', 'resync_highlight', true],
			['highlight', 'highlight.gif','change_highlight', true],
			['help', 'help.gif', 'show_help', false],
			['save', 'save.gif', 'save', false],
			['load', 'load.gif', 'load', false],
			['fullscreen', 'fullscreen.gif', 'toggle_full_screen', false],
			['autocompletion', 'autocompletion.gif', 'toggle_autocompletion', true]
		];
			
	// navigator identification
	ua= navigator.userAgent;
	
	this.nav= new Object(); 
	
	this.nav['isMacOS'] = (ua.indexOf('Mac OS') != -1);
	
	this.nav['isIE'] = (navigator.appName == "Microsoft Internet Explorer");
	if(this.nav['isIE']){
		this.nav['isIE'] = ua.replace(/^.*?MSIE ([0-9\.]*).*$/, "$1");
		if(this.nav['isIE']<6)
			this.has_error(); 
	}
	if(this.nav['isNS'] = ua.indexOf('Netscape/') != -1){	// work only on netscape > 8 with render mode IE
		this.nav['isNS']= ua.substr(ua.indexOf('Netscape/')+9);
		if(this.nav['isNS']<8 || !this.nav['isIE'])
			this.has_error();			
	}
	
	if(this.nav['isOpera'] = (ua.indexOf('Opera') != -1)){	
		this.nav['isOpera']= ua.replace(/^.*?Opera.*?([0-9\.]+).*$/i, "$1");
		if(this.nav['isOpera']<9)
			this.has_error();
		this.nav['isIE']=false;			
	}
	this.nav['isGecko'] = (ua.indexOf('Gecko') != -1);

	if(this.nav['isFirefox'] =(ua.indexOf('Firefox') != -1))
		this.nav['isFirefox'] = ua.replace(/^.*?Firefox.*?([0-9\.]+).*$/i, "$1");
	// Iceweasel is a clone of Firefox 	
	if(this.nav['isIceweasel'] =(ua.indexOf('Iceweasel') != -1))
		this.nav['isFirefox']= this.nav['isIceweasel'] = ua.replace(/^.*?Iceweasel.*?([0-9\.]+).*$/i, "$1");
	// grandparadisio is a clone of Firefox 	
	if(this.nav['GranParadiso'] =(ua.indexOf('GranParadiso') != -1))
		this.nav['isFirefox']= this.nav['isGranParadiso'] = ua.replace(/^.*?GranParadiso.*?([0-9\.]+).*$/i, "$1");
	// BonEcho is a clone of Firefox
	if(this.nav['BonEcho'] =(ua.indexOf('BonEcho') != -1))
		this.nav['isFirefox']= this.nav['isBonEcho'] = ua.replace(/^.*?BonEcho.*?([0-9\.]+).*$/i, "$1");
		
	if(this.nav['isCamino'] =(ua.indexOf('Camino') != -1))
		this.nav['isCamino'] = ua.replace(/^.*?Camino.*?([0-9\.]+).*$/i, "$1");

	if(this.nav['isChrome'] =(ua.indexOf('Chrome') != -1))
		this.nav['isChrome'] = ua.replace(/^.*?Chrome.*?([0-9\.]+).*$/i, "$1");
	
	if(this.nav['isSafari'] =(ua.indexOf('Safari') != -1))
		this.nav['isSafari']= ua.replace(/^.*?Version\/([0-9]+\.[0-9]+).*$/i, "$1");
	
	if(this.nav['isIE']>=6 || this.nav['isOpera']>=9 || this.nav['isFirefox'] || this.nav['isChrome'] || this.nav['isCamino'] || this.nav['isSafari']>=3)
		this.nav['isValidBrowser']=true;
	else
		this.nav['isValidBrowser']=false;

	this.set_base_url();		
	
	for(var i=0; i<this.scripts_to_load.length; i++){
		setTimeout("editAreaLoader.load_script('"+this.baseURL + this.scripts_to_load[i]+ ".js');", 1);	// let the time to Object editAreaLoader to be created before loading additionnal scripts
		this.waiting_loading[this.scripts_to_load[i]+ ".js"]= false;
	}				
	this.add_event(window, "load", EditAreaLoader.prototype.window_loaded);
};
	
EditAreaLoader.prototype ={
	has_error : function(){
		this.error= true;
		// set to empty all EditAreaLoader functions
		for(var i in EditAreaLoader.prototype){
			EditAreaLoader.prototype[i]=function(){};		
		}
	},
	
	window_loaded : function(){
		editAreaLoader.win="loaded";
		
		// add events on forms
		if (document.forms) {
			for (var i=0; i<document.forms.length; i++) {
				var form = document.forms[i];
				form.edit_area_replaced_submit=null;
				try {
					
					form.edit_area_replaced_submit = form.onsubmit;
					form.onsubmit="";
				} catch (e) {// Do nothing
				}
				editAreaLoader.add_event(form, "submit", EditAreaLoader.prototype.submit);
				editAreaLoader.add_event(form, "reset", EditAreaLoader.prototype.reset);
			}
		}
		editAreaLoader.add_event(window, "unload", function(){for(var i in editAreas){editAreaLoader.delete_instance(i);}});	// ini callback
	},
	
	// init the checkup of the selection of the IE textarea
	init_ie_textarea : function(id){
		var t=document.getElementById(id);
		try{
			if(t && typeof(t.focused)=="undefined"){
				t.focus();
				t.focused=true;
				t.selectionStart= t.selectionEnd= 0;			
				get_IE_selection(t);
				editAreaLoader.add_event(t, "focus", IE_textarea_focus);
				editAreaLoader.add_event(t, "blur", IE_textarea_blur);
				
			}
		}catch(ex){}
	},
		
	init : function(settings){
	
		if(!settings["id"])
			this.has_error();
		
		if(this.error)
			return;
		// if an instance of the editor already exists for this textarea => delete the previous one
		if(editAreas[settings["id"]])
			editAreaLoader.delete_instance(settings["id"]);
	
		// init settings
		for(var i in this.default_settings){
			if(typeof(settings[i])=="undefined")
				settings[i]=this.default_settings[i];
		}
		
		if(settings["browsers"]=="known" && this.nav['isValidBrowser']==false){
			return;
		}
		
		if(settings["begin_toolbar"].length>0)
			settings["toolbar"]= settings["begin_toolbar"] +","+ settings["toolbar"];
		if(settings["end_toolbar"].length>0)
			settings["toolbar"]= settings["toolbar"] +","+ settings["end_toolbar"];
		settings["tab_toolbar"]= settings["toolbar"].replace(/ /g,"").split(",");
		
		settings["plugins"]= settings["plugins"].replace(/ /g,"").split(",");
		for(var i=0; i<settings["plugins"].length; i++){
			if(settings["plugins"][i].length==0)
				settings["plugins"].splice(i,1);
		}
	//	alert(settings["plugins"].length+": "+ settings["plugins"].join(","));
	/*	var tmp="";
		for(var i in settings){
			tmp+=i+" : "+settings[i]+";\n";
		}
		alert(tmp);*/
		this.get_template();
		this.load_script(this.baseURL + "langs/"+ settings["language"] + ".js");
		
		if(settings["syntax"].length>0){
			settings["syntax"]=settings["syntax"].toLowerCase();
			this.load_script(this.baseURL + "reg_syntax/"+ settings["syntax"] + ".js");
		}
		//alert(this.template);
		
		editAreas[settings["id"]]= {"settings": settings};
		editAreas[settings["id"]]["displayed"]=false;
		editAreas[settings["id"]]["hidden"]=false;
		
		//if(settings["display"]=="onload")
		editAreaLoader.start(settings["id"]);
	},
	
	// delete an instance of an EditArea
	delete_instance : function(id){
		
		editAreaLoader.execCommand(id, "EA_delete");
		if(window.frames["frame_"+id] && window.frames["frame_"+id].editArea)
		{
			if(editAreas[id]["displayed"])
				editAreaLoader.toggle(id, "off");
			window.frames["frame_"+id].editArea.execCommand("EA_unload");
		}

		// remove toggle infos and debug textarea
		var span= document.getElementById("EditAreaArroundInfos_"+id);
		if(span)
			span.parentNode.removeChild(span);

		// remove the iframe
		var iframe= document.getElementById("frame_"+id);
		if(iframe){
			iframe.parentNode.removeChild(iframe);
			//delete iframe;
			try {
				delete window.frames["frame_"+id];
			} catch (e) {// Do nothing
			}
		}	

		delete editAreas[id];

	},

	
	start : function(id){
		// check that the window is loaded
		if(this.win!="loaded"){
			setTimeout("editAreaLoader.start('"+id+"');", 50);
			return;
		}
		
		// check that all needed scripts are loaded
		for(var i in editAreaLoader.waiting_loading){
			if(editAreaLoader.waiting_loading[i]!="loaded" && typeof(editAreaLoader.waiting_loading[i])!="function"){
				setTimeout("editAreaLoader.start('"+id+"');", 50);
				return;
			}
		}
		
		// wait until language and syntax files are loaded
		if(!editAreaLoader.lang[editAreas[id]["settings"]["language"]] || (editAreas[id]["settings"]["syntax"].length>0 && !editAreaLoader.load_syntax[editAreas[id]["settings"]["syntax"]]) ){
			setTimeout("editAreaLoader.start('"+id+"');", 50);
			return;
		}
		// init the regexp for syntax highlight
		if(editAreas[id]["settings"]["syntax"].length>0)
			editAreaLoader.init_syntax_regexp();
		
			
		// display toggle option and debug area
		if(!document.getElementById("EditAreaArroundInfos_"+id) && (editAreas[id]["settings"]["debug"] || editAreas[id]["settings"]["allow_toggle"]))
		{
			var span= document.createElement("span");
			span.id= "EditAreaArroundInfos_"+id;
			var html="";
			if(editAreas[id]["settings"]["allow_toggle"]){
				checked=(editAreas[id]["settings"]["display"]=="onload")?"checked":"";
				html+="<div id='edit_area_toggle_"+i+"'>";
				html+="<input id='edit_area_toggle_checkbox_"+ id +"' class='toggle_"+ id +"' type='checkbox' onclick='editAreaLoader.toggle(\""+ id +"\");' accesskey='e' "+checked+" />";
				html+="<label for='edit_area_toggle_checkbox_"+ id +"'>{$toggle}</label></div>";	
			}
			if(editAreas[id]["settings"]["debug"])
				html+="<textarea id='edit_area_debug_"+ id +"' style='z-index: 20; width: 100%; height: 120px;overflow: auto; border: solid black 1px;'></textarea><br />";				
			html= editAreaLoader.translate(html, editAreas[id]["settings"]["language"]);				
			span.innerHTML= html;				
			var father= document.getElementById(id).parentNode;
			var next= document.getElementById(id).nextSibling;
			if(next==null)
				father.appendChild(span);
			else
				father.insertBefore(span, next);
		}
		
		if(!editAreas[id]["initialized"])
		{
			this.execCommand(id, "EA_init");	// ini callback
			if(editAreas[id]["settings"]["display"]=="later"){
				editAreas[id]["initialized"]= true;
				return;
			}
		}
		
		if(this.nav['isIE']){	// launch IE selection checkup
			editAreaLoader.init_ie_textarea(id);
		}
				
		// get toolbar content
		var html_toolbar_content="";
		area=editAreas[id];
		
		for(var i=0; i<area["settings"]["tab_toolbar"].length; i++){
		//	alert(this.tab_toolbar[i]+"\n"+ this.get_control_html(this.tab_toolbar[i]));
			html_toolbar_content+= this.get_control_html(area["settings"]["tab_toolbar"][i], area["settings"]["language"]);
		}
		
		// create javascript import rules for the iframe if the javascript has not been already loaded by the compressor
		if(!this.iframe_script){
			this.iframe_script="";
			for(var i=0; i<this.sub_scripts_to_load.length; i++)
				this.iframe_script+='<script language="javascript" type="text/javascript" src="'+ this.baseURL + this.sub_scripts_to_load[i] +'.js"></script>';
		}
		
		// add plugins scripts if not already loaded by the compressor (but need to load language in all the case)
		for(var i=0; i<area["settings"]["plugins"].length; i++){
			//if(typeof(area["settings"]["plugins"][i])=="function") continue;
			if(!editAreaLoader.all_plugins_loaded)
				this.iframe_script+='<script language="javascript" type="text/javascript" src="'+ this.baseURL + 'plugins/' + area["settings"]["plugins"][i] + '/' + area["settings"]["plugins"][i] +'.js"></script>';
			this.iframe_script+='<script language="javascript" type="text/javascript" src="'+ this.baseURL + 'plugins/' + area["settings"]["plugins"][i] + '/langs/' + area["settings"]["language"] +'.js"></script>';
		}
	
		
		// create css link for the iframe if the whole css text has not been already loaded by the compressor
		if(!this.iframe_css){
			this.iframe_css="<link href='"+ this.baseURL +"edit_area.css' rel='stylesheet' type='text/css' />";
		}
		
		
		// create template
		var template= this.template.replace(/\[__BASEURL__\]/g, this.baseURL);
		template= template.replace("[__TOOLBAR__]",html_toolbar_content);
			
		
		// fill template with good language sentences
		template= this.translate(template, area["settings"]["language"], "template");
		
		// add css_code
		template= template.replace("[__CSSRULES__]", this.iframe_css);				
		// add js_code
		template= template.replace("[__JSCODE__]", this.iframe_script);
		
		// add version_code
		template= template.replace("[__EA_VERSION__]", this.version);
		//template=template.replace(/\{\$([^\}]+)\}/gm, this.traduc_template);
		
		//editAreas[area["settings"]["id"]]["template"]= template;
		
		area.textarea=document.getElementById(area["settings"]["id"]);
		editAreas[area["settings"]["id"]]["textarea"]=area.textarea;
	
		// if removing previous instances from DOM before (fix from Marcin)
		if(typeof(window.frames["frame_"+area["settings"]["id"]])!='undefined') 
			delete window.frames["frame_"+area["settings"]["id"]];
		
		// insert template in the document after the textarea
		var father= area.textarea.parentNode;
	/*	var container= document.createElement("div");
		container.id= "EditArea_frame_container_"+area["settings"]["id"];
	*/	
		var content= document.createElement("iframe");
		content.name= "frame_"+area["settings"]["id"];
		content.id= "frame_"+area["settings"]["id"];
		content.style.borderWidth= "0px";
		setAttribute(content, "frameBorder", "0"); // IE
		content.style.overflow="hidden";
		content.style.display="none";
		
	/*	container.appendChild(content);
		var next= area.textarea.nextSibling;
		if(next==null)
			father.appendChild(container);
		else
			father.insertBefore(container, next) ;*/
		
		var next= area.textarea.nextSibling;
		if(next==null)
			father.appendChild(content);
		else
			father.insertBefore(content, next) ;		
		var frame=window.frames["frame_"+area["settings"]["id"]];		
		
		frame.document.open();
		frame.editAreas=editAreas;
		frame.area_id= area["settings"]["id"];	
		frame.document.area_id= area["settings"]["id"];	
		frame.document.write(template);
		frame.document.close();

	//	frame.editAreaLoader=this;
		//editAreas[area["settings"]["id"]]["displayed"]=true;
		
	},
	
	toggle : function(id, toggle_to){

	/*	if((editAreas[id]["displayed"]==true  && toggle_to!="on") || toggle_to=="off"){
			this.toggle_off(id);
		}else if((editAreas[id]["displayed"]==false  && toggle_to!="off") || toggle_to=="on"){
			this.toggle_on(id);
		}*/
		if(!toggle_to)
			toggle_to= (editAreas[id]["displayed"]==true)?"off":"on";
		if(editAreas[id]["displayed"]==true  && toggle_to=="off"){
			this.toggle_off(id);
		}else if(editAreas[id]["displayed"]==false  && toggle_to=="on"){
			this.toggle_on(id);
		}
	
		return false;
	},
	
	toggle_off : function(id){
		if(window.frames["frame_"+id])
		{	
			var frame=window.frames["frame_"+id];
			if(frame.editArea.fullscreen['isFull'])
				frame.editArea.toggle_full_screen(false);
			editAreas[id]["displayed"]=false;
			
			// set wrap to off to keep same display mode (some browser get problem with this, so it need more complex operation
			
			editAreas[id]["textarea"].wrap = "off";	// for IE
			setAttribute(editAreas[id]["textarea"], "wrap", "off");	// for Firefox	
			var parNod = editAreas[id]["textarea"].parentNode;
			var nxtSib = editAreas[id]["textarea"].nextSibling;
			parNod.removeChild(editAreas[id]["textarea"]); 
			parNod.insertBefore(editAreas[id]["textarea"], nxtSib);
			
			// restore values
			editAreas[id]["textarea"].value= frame.editArea.textarea.value;
			var selStart= frame.editArea.last_selection["selectionStart"];
			var selEnd= frame.editArea.last_selection["selectionEnd"];
			var scrollTop= frame.document.getElementById("result").scrollTop;
			var scrollLeft= frame.document.getElementById("result").scrollLeft;
			
			
			document.getElementById("frame_"+id).style.display='none';
		
			editAreas[id]["textarea"].style.display="inline";


			editAreas[id]["textarea"].focus();	
			if(this.nav['isIE']){
				editAreas[id]["textarea"].selectionStart= selStart;
				editAreas[id]["textarea"].selectionEnd= selEnd;
				editAreas[id]["textarea"].focused=true;
				set_IE_selection(editAreas[id]["textarea"]);
			}else{
				if(this.nav['isOpera']){	// Opera bug when moving selection start and selection end
					editAreas[id]["textarea"].setSelectionRange(0, 0);
				}
				try{
					editAreas[id]["textarea"].setSelectionRange(selStart, selEnd);
				} catch(e) {
				};
			}
			editAreas[id]["textarea"].scrollTop= scrollTop;
			editAreas[id]["textarea"].scrollLeft= scrollLeft;
			frame.editArea.execCommand("toggle_off");

		}
	},	
	
	toggle_on : function(id){
		/*if(!editAreas[area["settings"]["id"]]["started"])
			editAreaLoader.start(id);*/
			
		if(window.frames["frame_"+id])
		{
			var frame=window.frames["frame_"+id];
			area= frame.editArea;
			area.textarea.value= editAreas[id]["textarea"].value;
			
			// store display values;
			var selStart= 0;
			var selEnd= 0;
			var scrollTop= 0;
			var scrollLeft= 0;
			var curPos	= editAreas[id]["settings"]["cursor_position"];

			if(editAreas[id]["textarea"].use_last==true)
			{
				var selStart= editAreas[id]["textarea"].last_selectionStart;
				var selEnd= editAreas[id]["textarea"].last_selectionEnd;
				var scrollTop= editAreas[id]["textarea"].last_scrollTop;
				var scrollLeft= editAreas[id]["textarea"].last_scrollLeft;
				editAreas[id]["textarea"].use_last=false;
			}
			else if( curPos == "auto" )
			{
				try{
					var selStart= editAreas[id]["textarea"].selectionStart;
					var selEnd= editAreas[id]["textarea"].selectionEnd;
					var scrollTop= editAreas[id]["textarea"].scrollTop;
					var scrollLeft= editAreas[id]["textarea"].scrollLeft;
					//alert(scrollTop);
				}catch(ex){}
			}
			
			// set to good size
			this.set_editarea_size_from_textarea(id, document.getElementById("frame_"+id));
			editAreas[id]["textarea"].style.display="none";			
			document.getElementById("frame_"+id).style.display="inline";
			area.execCommand("focus"); // without this focus opera doesn't manage well the iframe body height
			
			
			// restore display values
			editAreas[id]["displayed"]=true;
			area.execCommand("update_size");
			
			frame.document.getElementById("result").scrollTop= scrollTop;
			frame.document.getElementById("result").scrollLeft= scrollLeft;
			area.area_select(selStart, selEnd-selStart);
			area.execCommand("toggle_on");

			/*date= new Date();
			end_time=date.getTime();		
			alert("load time: "+ (end_time-this.start_time));*/
			
		}
		else
		{
		/*	if(this.nav['isIE'])
				get_IE_selection(document.getElementById(id));	*/	
			var elem= document.getElementById(id);	
			elem.last_selectionStart= elem.selectionStart;
			elem.last_selectionEnd= elem.selectionEnd;
			elem.last_scrollTop= elem.scrollTop;
			elem.last_scrollLeft= elem.scrollLeft;
			elem.use_last=true;
			editAreaLoader.start(id);
		}
	},	
	
	set_editarea_size_from_textarea : function(id, frame){	
		var elem= document.getElementById(id);
		//var width= elem.offsetWidth+"px";
		//var height= elem.offsetHeight+"px";
		var width=Math.max(editAreas[id]["settings"]["min_width"], elem.offsetWidth)+"px";
		var height=Math.max(editAreas[id]["settings"]["min_height"], elem.offsetHeight)+"px";
		if(elem.style.width.indexOf("%")!=-1)
			width= elem.style.width;
		if(elem.style.height.indexOf("%")!=-1)
			height= elem.style.height;
		//alert("h: "+height+" w: "+width);
	
		frame.style.width= width;
		frame.style.height= height;
	},
		
	set_base_url : function(){
		//this.baseURL="";
		if (!this.baseURL) {
			var elements = document.getElementsByTagName('script');
	
			for (var i=0; i<elements.length; i++) {
				if (elements[i].src && elements[i].src.match(/edit_area_[^\\\/]*$/i) ) {
					var src = elements[i].src;
					src = src.substring(0, src.lastIndexOf('/'));
					this.baseURL = src;
					this.file_name= elements[i].src.substr(elements[i].src.lastIndexOf("/")+1);
					break;
				}
			}
		}
		
		var documentBasePath = document.location.href;
		if (documentBasePath.indexOf('?') != -1)
			documentBasePath = documentBasePath.substring(0, documentBasePath.indexOf('?'));
		var documentURL = documentBasePath;
		documentBasePath = documentBasePath.substring(0, documentBasePath.lastIndexOf('/'));
	
		// If not HTTP absolute
		if (this.baseURL.indexOf('://') == -1 && this.baseURL.charAt(0) != '/') {
			// If site absolute
			this.baseURL = documentBasePath + "/" + this.baseURL;
		}
		this.baseURL+="/";	
	},
	
	get_button_html : function(id, img, exec, isFileSpecific, baseURL) {
		if(!baseURL)
			baseURL= this.baseURL;
		var cmd = 'editArea.execCommand(\'' + exec + '\')';
		html= '<a id="a_'+ id +'" href="javascript:' + cmd + '" onclick="' + cmd + ';return false;" onmousedown="return false;" target="_self" fileSpecific="'+ (isFileSpecific?'yes':'no') +'">';
		html+= '<img id="' + id + '" src="'+ baseURL +'images/' + img + '" title="{$' + id + '}" width="20" height="20" class="editAreaButtonNormal" onmouseover="editArea.switchClass(this,\'editAreaButtonOver\');" onmouseout="editArea.restoreClass(this);" onmousedown="editArea.restoreAndSwitchClass(this,\'editAreaButtonDown\');" /></a>';
		return html;
	},

	get_control_html : function(button_name, lang) {		
		
		for (var i=0; i<this.advanced_buttons.length; i++)
		{
			var but = this.advanced_buttons[i];			
			if (but[0] == button_name)
			{
				return this.get_button_html(but[0], but[1], but[2], but[3]);
			}	
		}		
				
		switch (button_name){
			case "*":
			case "return":
				return "<br />";
			case "|":
		  	case "separator":
				return '<img src="'+ this.baseURL +'images/spacer.gif" width="1" height="15" class="editAreaSeparatorLine">';
			case "select_font":
				html= "<select id='area_font_size' onchange='javascript:editArea.execCommand(\"change_font_size\")' fileSpecific='yes'>"
					+"<option value='-1'>{$font_size}</option>"
					+"<option value='8'>8 pt</option>"
					+"<option value='9'>9 pt</option>"
					+"<option value='10'>10 pt</option>"
					+"<option value='11'>11 pt</option>"
					+"<option value='12'>12 pt</option>"
					+"<option value='14'>14 pt</option>"
					+"</select>";
				return html;
			case "syntax_selection":
				var html= "<select id='syntax_selection' onchange='javascript:editArea.execCommand(\"change_syntax\", this.value)' fileSpecific='yes'>";
				html+="<option value='-1'>{$syntax_selection}</option>";
				html+="</select>";
				return html;
		}
		
		return "<span id='tmp_tool_"+button_name+"'>["+button_name+"]</span>";		
	},
	
	
	get_template : function(){
		if(this.template=="")
		{
			var xhr_object = null; 
			if(window.XMLHttpRequest) // Firefox 
				xhr_object = new XMLHttpRequest(); 
			else if(window.ActiveXObject) // Internet Explorer 
				xhr_object = new ActiveXObject("Microsoft.XMLHTTP"); 
			else { // XMLHttpRequest not supported
				alert("XMLHTTPRequest not supported. EditArea not loaded"); 
				return; 
			} 
			 
			xhr_object.open("GET", this.baseURL+"template.html", false); 
			xhr_object.send(null); 
			if(xhr_object.readyState == 4) 
				this.template=xhr_object.responseText;
			else
				this.has_error();
		}
	},
	
	// translate text
	translate : function(text, lang, mode){
		
		if(mode=="word")
			text=editAreaLoader.get_word_translation(text, lang);
		else if(mode="template"){
			editAreaLoader.current_language= lang;
			text=text.replace(/\{\$([^\}]+)\}/gm, editAreaLoader.translate_template);
		}
		return text;
	},
	
	translate_template : function(){
		return editAreaLoader.get_word_translation(EditAreaLoader.prototype.translate_template.arguments[1], editAreaLoader.current_language);
	},
	
	get_word_translation : function(val, lang){
		for(var i in editAreaLoader.lang[lang]){
			if(i == val)
				return editAreaLoader.lang[lang][i];
		}
		return "_"+val;
	},
	
	load_script : function(url){
		if (this.loadedFiles[url])
			return;	
		//alert("load: "+url);
		try{
			var script= document.createElement("script");
			script.type= "text/javascript";
			script.src= url;
			script.charset= "UTF-8";
			var head= document.getElementsByTagName("head");
			head[0].appendChild(script);
		}catch(e){
			document.write('<sc'+'ript language="javascript" type="text/javascript" src="' + url + '" charset="UTF-8"></sc'+'ript>');
		}
		//var filename= url.replace(/^.*?\/?([a-z\.\_\-]+)$/i, "$1");
		this.loadedFiles[url] = true;
	},
	
	add_event : function(obj, name, handler) {
		if (obj.attachEvent) {
			obj.attachEvent("on" + name, handler);
		} else{
			obj.addEventListener(name, handler, false);
		}
	},
	
	remove_event : function(obj, name, handler){
		if (obj.detachEvent)
			obj.detachEvent("on" + name, handler);
		else
			obj.removeEventListener(name, handler, false);
	},


	// reset all the editareas in the form that have been reseted
	reset : function(e){
		var formObj = editAreaLoader.nav['isIE'] ? window.event.srcElement : e.target;
		if(formObj.tagName!='FORM')
			formObj= formObj.form;
		
		for(var i in editAreas){			
			var is_child= false;
			for (var x=0;x<formObj.elements.length;x++) {
				if(formObj.elements[x].id == i)
					is_child=true;
			}
			
			if(window.frames["frame_"+i] && is_child && editAreas[i]["displayed"]==true){
			
				var exec= 'window.frames["frame_'+ i +'"].editArea.textarea.value= document.getElementById("'+ i +'").value;';
				exec+= 'window.frames["frame_'+ i +'"].editArea.execCommand("focus");';
				exec+= 'window.frames["frame_'+ i +'"].editArea.check_line_selection();';
				exec+= 'window.frames["frame_'+ i +'"].editArea.execCommand("reset");';
				window.setTimeout(exec, 10);
			}
		}		
		return;
	},
	
	
	// prepare all the textarea replaced by an editarea to be submited
	submit : function(e){		
		var formObj = editAreaLoader.nav['isIE'] ? window.event.srcElement : e.target;
		if(formObj.tagName!='FORM')
			formObj= formObj.form;
		
		for(var i in editAreas){
			var is_child= false;
			for (var x=0;x<formObj.elements.length;x++) {
				if(formObj.elements[x].id == i)
					is_child=true;
			}
		
			if(is_child)
			{
				if(window.frames["frame_"+i] && editAreas[i]["displayed"]==true)
					document.getElementById(i).value= window.frames["frame_"+ i].editArea.textarea.value;
				editAreaLoader.execCommand(i,"EA_submit");
			}
		}				
		if(typeof(formObj.edit_area_replaced_submit) == "function"){
			res= formObj.edit_area_replaced_submit();
			if(res==false){
				if(editAreaLoader.nav['isIE'])
					return false;
				else
					e.preventDefault();
			}
		}
		return ;
	},
	
	// allow to get the value of the editarea
	getValue : function(id){
        if(window.frames["frame_"+id] && editAreas[id]["displayed"]==true){
            return window.frames["frame_"+ id].editArea.textarea.value;       
        }else if(elem=document.getElementById(id)){
        	return elem.value;
        }
        return false;
    },
    
    // allow to set the value of the editarea
    setValue : function(id, new_val){
        if(window.frames["frame_"+id] && editAreas[id]["displayed"]==true){
            window.frames["frame_"+ id].editArea.textarea.value= new_val;     
			window.frames["frame_"+ id].editArea.execCommand("focus"); 
			window.frames["frame_"+ id].editArea.check_line_selection(false);  
			window.frames["frame_"+ id].editArea.execCommand("onchange");
        }else if(elem=document.getElementById(id)){
        	elem.value= new_val;
        }
    },
	    
    // allow to get infos on the selection: array(start, end)
    getSelectionRange : function(id){
    	var sel= {"start": 0, "end": 0};
        if(window.frames["frame_"+id] && editAreas[id]["displayed"]==true){
        	var editArea= window.frames["frame_"+ id].editArea;
           
		/*	if(this.nav['isIE']){
				editArea.getIESelection();
				
			}
			sel["start"]=editArea.textarea.selectionStart;
			sel["end"]=editArea.textarea.selectionEnd;
		*/
			sel["start"]=editArea.textarea.selectionStart;
			sel["end"]=editArea.textarea.selectionEnd;
		
        }else if(elem=document.getElementById(id)){
        	sel= getSelectionRange(elem);
        }
        return sel;
    },
    
    // allow to set the selection with the given start and end positions
    setSelectionRange : function(id, new_start, new_end){
        if(window.frames["frame_"+id] && editAreas[id]["displayed"]==true){
            window.frames["frame_"+ id].editArea.area_select(new_start, new_end-new_start);  
			// make an auto-scroll to the selection
			if(!this.nav['isIE']){
				window.frames["frame_"+ id].editArea.check_line_selection(false); 
				window.frames["frame_"+ id].editArea.scroll_to_view();
			}   
        }else if(elem=document.getElementById(id)){
        	setSelectionRange(elem, new_start, new_end);
        }
    },
    
    getSelectedText : function(id){
    	var sel= this.getSelectionRange(id);
        return this.getValue(id).substring(sel["start"], sel["end"]);
    },
	
	setSelectedText : function(id, new_val){
		new_val= new_val.replace(/\r/g, ""); 
		var sel= this.getSelectionRange(id);
		var text= this.getValue(id);
		if(window.frames["frame_"+id] && editAreas[id]["displayed"]==true){
			var scrollTop= window.frames["frame_"+ id].document.getElementById("result").scrollTop;
			var scrollLeft= window.frames["frame_"+ id].document.getElementById("result").scrollLeft;
		}else{
			var scrollTop= document.getElementById(id).scrollTop;
			var scrollLeft= document.getElementById(id).scrollLeft;
		}
		
		text= text.substring(0, sel["start"])+ new_val +text.substring(sel["end"]);
		this.setValue(id, text);
		var new_sel_end= sel["start"]+ new_val.length;
		this.setSelectionRange(id, sel["start"], new_sel_end);
		
		
		// fix \r problem for selection length count on IE & Opera
		if(new_val != this.getSelectedText(id).replace(/\r/g, "")){
			this.setSelectionRange(id, sel["start"], new_sel_end+ new_val.split("\n").length -1);
		}
		// restore scrolling position
		if(window.frames["frame_"+id] && editAreas[id]["displayed"]==true){
			window.frames["frame_"+ id].document.getElementById("result").scrollTop= scrollTop;
			window.frames["frame_"+ id].document.getElementById("result").scrollLeft= scrollLeft;
			window.frames["frame_"+ id].editArea.execCommand("onchange");
		}else{
			document.getElementById(id).scrollTop= scrollTop;
			document.getElementById(id).scrollLeft= scrollLeft;
		}
    },
    
    insertTags : function(id, open_tag, close_tag){
    	var old_sel= this.getSelectionRange(id);
    	text= open_tag + this.getSelectedText(id) + close_tag; 
		editAreaLoader.setSelectedText(id, text);
    	var new_sel= this.getSelectionRange(id);
    	if(old_sel["end"] > old_sel["start"])	// if text was selected, cursor at the end
    		this.setSelectionRange(id, new_sel["end"], new_sel["end"]);
    	else // cursor in the middle
    		this.setSelectionRange(id, old_sel["start"]+open_tag.length, old_sel["start"]+open_tag.length);
    },
    
    // hide both EditArea and normal textarea
	hide : function(id){
		if(document.getElementById(id) && !this.hidden[id])
		{
			this.hidden[id]= new Object();
			this.hidden[id]["selectionRange"]= this.getSelectionRange(id);
			if(document.getElementById(id).style.display!="none")
			{
				this.hidden[id]["scrollTop"]= document.getElementById(id).scrollTop;
				this.hidden[id]["scrollLeft"]= document.getElementById(id).scrollLeft;
			}
					
			if(window.frames["frame_"+id])
			{
				this.hidden[id]["toggle"]= editAreas[id]["displayed"];
				
				if(window.frames["frame_"+id] && editAreas[id]["displayed"]==true){
					var scrollTop= window.frames["frame_"+ id].document.getElementById("result").scrollTop;
					var scrollLeft= window.frames["frame_"+ id].document.getElementById("result").scrollLeft;
				}else{
					var scrollTop= document.getElementById(id).scrollTop;
					var scrollLeft= document.getElementById(id).scrollLeft;
				}
				this.hidden[id]["scrollTop"]= scrollTop;
				this.hidden[id]["scrollLeft"]= scrollLeft;
				
				if(editAreas[id]["displayed"]==true)
					editAreaLoader.toggle_off(id);
			}
			
			// hide toggle button and debug box
			var span= document.getElementById("EditAreaArroundInfos_"+id);
			if(span){
				span.style.display='none';
			}
			
			// hide textarea
			document.getElementById(id).style.display= "none";
		}
	},
	
	// restore hidden EditArea and normal textarea
	show : function(id){
		if((elem=document.getElementById(id)) && this.hidden[id])
		{
			elem.style.display= "inline";
			elem.scrollTop= this.hidden[id]["scrollTop"];
			elem.scrollLeft= this.hidden[id]["scrollLeft"];
			var span= document.getElementById("EditAreaArroundInfos_"+id);
			if(span){
				span.style.display='inline';
			}
			
			if(window.frames["frame_"+id])
			{
								
				// restore toggle button and debug box
			
				
				// restore textarea
				elem.style.display= "inline";
				
				// restore EditArea
				if(this.hidden[id]["toggle"]==true)
					editAreaLoader.toggle_on(id);
				
				scrollTop= this.hidden[id]["scrollTop"];
				scrollLeft= this.hidden[id]["scrollLeft"];
				
				if(window.frames["frame_"+id] && editAreas[id]["displayed"]==true){
					window.frames["frame_"+ id].document.getElementById("result").scrollTop= scrollTop;
					window.frames["frame_"+ id].document.getElementById("result").scrollLeft= scrollLeft;
				}else{
					elem.scrollTop= scrollTop;
					elem.scrollLeft= scrollLeft;
				}
			
			}
			// restore selection
			sel= this.hidden[id]["selectionRange"];
			this.setSelectionRange(id, sel["start"], sel["end"]);
			delete this.hidden[id];	
		}
	},
	
	// get the current file datas (for multi file editing mode)
	getCurrentFile : function(id){
		return this.execCommand(id, 'get_file', this.execCommand(id, 'curr_file'));
	},
	
	// get the given file datas (for multi file editing mode)
	getFile : function(id, file_id){
		return this.execCommand(id, 'get_file', file_id);
	},
	
	// get all the openned files datas (for multi file editing mode)
	getAllFiles : function(id){
		return this.execCommand(id, 'get_all_files()');
	},
	
	// open a file (for multi file editing mode)
	openFile : function(id, file_infos){
		return this.execCommand(id, 'open_file', file_infos);
	},
	
	// close the given file (for multi file editing mode)
	closeFile : function(id, file_id){
		return this.execCommand(id, 'close_file', file_id);
	},
	
	// close the given file (for multi file editing mode)
	setFileEditedMode : function(id, file_id, to){
		var reg1= new RegExp('\\\\', 'g');
		var reg2= new RegExp('"', 'g');
		return this.execCommand(id, 'set_file_edited_mode("'+ file_id.replace(reg1, '\\\\').replace(reg2, '\\"') +'", '+ to +')');
	},
	
	
	// allow to access to editarea functions and datas (for advanced users only)
	execCommand : function(id, cmd, fct_param){
		switch(cmd){
			case "EA_init":
				if(editAreas[id]['settings']["EA_init_callback"].length>0)
					eval(editAreas[id]['settings']["EA_init_callback"]+"('"+ id +"');");
				break;
			case "EA_delete":
				if(editAreas[id]['settings']["EA_delete_callback"].length>0)
					eval(editAreas[id]['settings']["EA_delete_callback"]+"('"+ id +"');");
				break;
			case "EA_submit":
				if(editAreas[id]['settings']["submit_callback"].length>0)
					eval(editAreas[id]['settings']["submit_callback"]+"('"+ id +"');");
				break;
		}
        if(window.frames["frame_"+id] && window.frames["frame_"+ id].editArea){
			if(fct_param!=undefined)
				return eval('window.frames["frame_'+ id +'"].editArea.'+ cmd +'(fct_param);');
			else
				return eval('window.frames["frame_'+ id +'"].editArea.'+ cmd +';');       
        }
        return false;
    }
};
	
	var editAreaLoader= new EditAreaLoader();
	var editAreas= new Object();

