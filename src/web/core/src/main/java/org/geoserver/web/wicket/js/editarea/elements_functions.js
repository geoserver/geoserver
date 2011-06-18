/****
 * This page contains some general usefull functions for javascript
 *
 ****/  
	
	
	// need to redefine this functiondue to IE problem
	function getAttribute( elm, aname ) {
		try{
			var avalue = elm.getAttribute( aname );
		}catch(exept){
		
		}
		if ( ! avalue ) {
			for ( var i = 0; i < elm.attributes.length; i ++ ) {
				var taName = elm.attributes [i] .name.toLowerCase();
				if ( taName == aname ) {
					avalue = elm.attributes [i] .value;
					return avalue;
				}
			}
		}
		return avalue;
	};
	
	// need to redefine this function due to IE problem
	function setAttribute( elm, attr, val ) {
		if(attr=="class"){
			elm.setAttribute("className", val);
			elm.setAttribute("class", val);
		}else{
			elm.setAttribute(attr, val);
		}
	};
	
	/* return a child element
		elem: element we are searching in
		elem_type: type of the eleemnt we are searching (DIV, A, etc...)
		elem_attribute: attribute of the searched element that must match
		elem_attribute_match: value that elem_attribute must match
		option: "all" if must return an array of all children, otherwise return the first match element
		depth: depth of search (-1 or no set => unlimited)
	*/
	function getChildren(elem, elem_type, elem_attribute, elem_attribute_match, option, depth)
	{           
		if(!option)
			var option="single";
		if(!depth)
			var depth=-1;
		if(elem){
			var children= elem.childNodes;
			var result=null;
			var results= new Array();
			for (var x=0;x<children.length;x++) {
				strTagName = new String(children[x].tagName);
				children_class="?";
				if(strTagName!= "undefined"){
					child_attribute= getAttribute(children[x],elem_attribute);
					if((strTagName.toLowerCase()==elem_type.toLowerCase() || elem_type=="") && (elem_attribute=="" || child_attribute==elem_attribute_match)){
						if(option=="all"){
							results.push(children[x]);
						}else{
							return children[x];
						}
					}
					if(depth!=0){
						result=getChildren(children[x], elem_type, elem_attribute, elem_attribute_match, option, depth-1);
						if(option=="all"){
							if(result.length>0){
								results= results.concat(result);
							}
						}else if(result!=null){                                                                          
							return result;
						}
					}
				}
			}
			if(option=="all")
			   return results;
		}
		return null;
	};       
	
	function isChildOf(elem, parent){
		if(elem){
			if(elem==parent)
				return true;
			while(elem.parentNode != 'undefined'){
				return isChildOf(elem.parentNode, parent);
			}
		}
		return false;
	};
	
	function getMouseX(e){
		/*if(document.all)
			return event.x + document.body.scrollLeft;
		else
			return e.pageX;*/
		if(e!=null && typeof(e.pageX)!="undefined"){
			return e.pageX;
		}else{
			return (e!=null?e.x:event.x)+ document.documentElement.scrollLeft;
		}
		//return (e!=null) ? e.pageX : event.x + document.documentElement.scrollLeft;
	};
	
	function getMouseY(e){
		/*if(document.all)
			return event.y + document.body.scrollTop;
		else
			return e.pageY;*/
		if(e!=null && typeof(e.pageY)!="undefined"){
			return e.pageY;
		}else{
			return (e!=null?e.y:event.y)+ document.documentElement.scrollTop;
		}
		//return (e!=null) ? e.pageY : event.y + document.documentElement.scrollTop;
	};
	
	function calculeOffsetLeft(r){
	  return calculeOffset(r,"offsetLeft")
	};
	
	function calculeOffsetTop(r){
	  return calculeOffset(r,"offsetTop")
	};
	
	function calculeOffset(element,attr){
	  var offset=0;
	  while(element){
		offset+=element[attr];
		element=element.offsetParent
	  }
	  return offset;
	};
	
	/** return the computed style
	 *	@param: elem: the reference to the element
	 *	@param: prop: the name of the css property	 
	 */
	function get_css_property(elem, prop)
	{
		if(document.defaultView)
		{
			return document.defaultView.getComputedStyle(elem, null).getPropertyValue(prop);
		}
		else if(elem.currentStyle)
		{
			var prop = prop.replace(/-\D/gi, function(sMatch)
			{
				return sMatch.charAt(sMatch.length - 1).toUpperCase();
			});
			return elem.currentStyle[prop];
		}
		else return null;
	}
	
/****
 * Moving an element 
 ***/  
	
	var move_current_element;
	/* allow to move an element in a window
		e: the event
		id: the id of the element
		frame: the frame of the element 
		ex of use:
			in html:	<img id='move_area_search_replace' onmousedown='return parent.start_move_element(event,"area_search_replace", parent.frames["this_frame_id"]);' .../>  
		or
			in javascript: document.getElementById("my_div").onmousedown= start_move_element
	*/
	function start_move_element(e, id, frame){
		var elem_id=(e.target || e.srcElement).id;
		if(id)
			elem_id=id;		
		if(!frame)
			frame=window;
		if(frame.event)
			e=frame.event;
			
		move_current_element= frame.document.getElementById(elem_id);
		move_current_element.frame=frame;
		frame.document.onmousemove= move_element;
		frame.document.onmouseup= end_move_element;
		/*move_current_element.onmousemove= move_element;
		move_current_element.onmouseup= end_move_element;*/
		
		//alert(move_current_element.frame.document.body.offsetHeight);
		
		mouse_x= getMouseX(e);
		mouse_y= getMouseY(e);
		//window.status=frame+ " elem: "+elem_id+" elem: "+ move_current_element + " mouse_x: "+mouse_x;
		move_current_element.start_pos_x = mouse_x - (move_current_element.style.left.replace("px","") || calculeOffsetLeft(move_current_element));
		move_current_element.start_pos_y = mouse_y - (move_current_element.style.top.replace("px","") || calculeOffsetTop(move_current_element));
		return false;
	};
	
	function end_move_element(e){
		move_current_element.frame.document.onmousemove= "";
		move_current_element.frame.document.onmouseup= "";		
		move_current_element=null;
	};
	
	function move_element(e){
		/*window.status="move"+frame;
		window.status="move2"+frame.event;*/
		if(move_current_element.frame && move_current_element.frame.event)
			e=move_current_element.frame.event;
		var mouse_x=getMouseX(e);
		var mouse_y=getMouseY(e);
		var new_top= mouse_y - move_current_element.start_pos_y;
		var new_left= mouse_x - move_current_element.start_pos_x;
		
		var max_left= move_current_element.frame.document.body.offsetWidth- move_current_element.offsetWidth;
		max_top= move_current_element.frame.document.body.offsetHeight- move_current_element.offsetHeight;
		new_top= Math.min(Math.max(0, new_top), max_top);
		new_left= Math.min(Math.max(0, new_left), max_left);
		
		move_current_element.style.top= new_top+"px";
		move_current_element.style.left= new_left+"px";		
		return false;
	};
	
/***
 * Managing a textarea (this part need the navigator infos from editAreaLoader
 ***/ 
	
	var nav= editAreaLoader.nav;
	
	// allow to get infos on the selection: array(start, end)
	function getSelectionRange(textarea){
		//if(nav['isIE'])
		//	get_IE_selection(textarea);
		return {"start": textarea.selectionStart, "end": textarea.selectionEnd};
	};
	
	// allow to set the selection
	function setSelectionRange(textarea, start, end){
		textarea.focus();
		
		start= Math.max(0, Math.min(textarea.value.length, start));
		end= Math.max(start, Math.min(textarea.value.length, end));
	
		if(nav['isOpera']){	// Opera bug when moving selection start and selection end
			textarea.selectionEnd = 1;	
			textarea.selectionStart = 0;			
			textarea.selectionEnd = 1;	
			textarea.selectionStart = 0;		
		}
		textarea.selectionStart = start;
		textarea.selectionEnd = end;		
		//textarea.setSelectionRange(start, end);
		
		if(nav['isIE'])
			set_IE_selection(textarea);
	};

	
	// set IE position in Firefox mode (textarea.selectionStart and textarea.selectionEnd). should work as a repeated task
	function get_IE_selection(textarea){
			
		if(textarea && textarea.focused)
		{	
			if(!textarea.ea_line_height)
			{	// calculate the lineHeight
				var div= document.createElement("div");
				div.style.fontFamily= get_css_property(textarea, "font-family");
				div.style.fontSize= get_css_property(textarea, "font-size");
				div.style.visibility= "hidden";			
				div.innerHTML="0";
				document.body.appendChild(div);
				textarea.ea_line_height= div.offsetHeight;
				document.body.removeChild(div);
			}
			//textarea.focus();
			var range = document.selection.createRange();
			try
			{
				var stored_range = range.duplicate();
				stored_range.moveToElementText( textarea );
				stored_range.setEndPoint( 'EndToEnd', range );
				if(stored_range.parentElement()==textarea){
					// the range don't take care of empty lines in the end of the selection
					var elem= textarea;
					var scrollTop= 0;
					while(elem.parentNode){
						scrollTop+= elem.scrollTop;
						elem= elem.parentNode;
					}
				
				//	var scrollTop= textarea.scrollTop + document.body.scrollTop;
					
				//	var relative_top= range.offsetTop - calculeOffsetTop(textarea) + scrollTop;
					var relative_top= range.offsetTop - calculeOffsetTop(textarea)+ scrollTop;
				//	alert("rangeoffset: "+ range.offsetTop +"\ncalcoffsetTop: "+ calculeOffsetTop(textarea) +"\nrelativeTop: "+ relative_top);
					var line_start = Math.round((relative_top / textarea.ea_line_height) +1);
					
					var line_nb= Math.round(range.boundingHeight / textarea.ea_line_height);
					
			//		alert("store_range: "+ stored_range.text.length+"\nrange: "+range.text.length+"\nrange_text: "+ range.text);
					var range_start= stored_range.text.length - range.text.length;
					var tab= textarea.value.substr(0, range_start).split("\n");			
					range_start+= (line_start - tab.length)*2;		// add missing empty lines to the selection
					textarea.selectionStart = range_start;
					
					var range_end= textarea.selectionStart + range.text.length;
					tab= textarea.value.substr(0, range_start + range.text.length).split("\n");			
					range_end+= (line_start + line_nb - 1 - tab.length)*2;
					textarea.selectionEnd = range_end;
				}
			}
			catch(e){}
		}
		setTimeout("get_IE_selection(document.getElementById('"+ textarea.id +"'));", 50);
	};
	
	function IE_textarea_focus(){
		event.srcElement.focused= true;
	}
	
	function IE_textarea_blur(){
		event.srcElement.focused= false;
	}
	
	// select the text for IE (take into account the \r difference)
	function set_IE_selection(textarea){
		if(!window.closed){ 
			var nbLineStart=textarea.value.substr(0, textarea.selectionStart).split("\n").length - 1;
			var nbLineEnd=textarea.value.substr(0, textarea.selectionEnd).split("\n").length - 1;
			try
			{
				var range = document.selection.createRange();
				range.moveToElementText( textarea );
				range.setEndPoint( 'EndToStart', range );
				range.moveStart('character', textarea.selectionStart - nbLineStart);
				range.moveEnd('character', textarea.selectionEnd - nbLineEnd - (textarea.selectionStart - nbLineStart)  );
				range.select();
			}
			catch(e){}
		}
	};
	
	
	editAreaLoader.waiting_loading["elements_functions.js"]= "loaded";
