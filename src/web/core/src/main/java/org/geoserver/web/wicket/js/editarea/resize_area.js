	
	EditAreaLoader.prototype.start_resize_area= function(){
		document.onmouseup= editAreaLoader.end_resize_area;
		document.onmousemove= editAreaLoader.resize_area;
		editAreaLoader.toggle(editAreaLoader.resize["id"]);		
		
		var textarea= editAreas[editAreaLoader.resize["id"]]["textarea"];
		var div= document.getElementById("edit_area_resize");
		if(!div){
			div= document.createElement("div");
			div.id="edit_area_resize";
			div.style.border="dashed #888888 1px";
		}
		var width= textarea.offsetWidth -2;
		var height= textarea.offsetHeight -2;
		/*if(this.nav['isGecko']){
			width-=2;
			height-=2;
		}*/
		
		div.style.display="block";
		div.style.width= width+"px";
		div.style.height= height+"px";
		var father= textarea.parentNode;
		father.insertBefore(div, textarea);
		
		textarea.style.display="none";
				
		editAreaLoader.resize["start_top"]= calculeOffsetTop(div);
		editAreaLoader.resize["start_left"]= calculeOffsetLeft(div);

		
		/*var next= textarea.nextSibling;
		if(next==null)
			father.appendChild(div);
		else
			father.insertBefore(div, next);*/
		
	};
	
	EditAreaLoader.prototype.end_resize_area= function(e){
		document.onmouseup="";
		document.onmousemove="";		
		
		var div= document.getElementById("edit_area_resize");		
		var textarea= editAreas[editAreaLoader.resize["id"]]["textarea"];
		var width= Math.max(editAreas[editAreaLoader.resize["id"]]["settings"]["min_width"], div.offsetWidth-4);
		var height= Math.max(editAreas[editAreaLoader.resize["id"]]["settings"]["min_height"], div.offsetHeight-4);
		if(editAreaLoader.nav['isIE']==6){
			width-=2;
			height-=2;	
		}
		textarea.style.width= width+"px";
		textarea.style.height= height+"px";
		div.style.display="none";
		textarea.style.display="inline";
		textarea.selectionStart= editAreaLoader.resize["selectionStart"];
		textarea.selectionEnd= editAreaLoader.resize["selectionEnd"];
		editAreaLoader.toggle(editAreaLoader.resize["id"]);
		
		return false;
	};
	
	EditAreaLoader.prototype.resize_area= function(e){		
		var allow= editAreas[editAreaLoader.resize["id"]]["settings"]["allow_resize"];
		if(allow=="both" || allow=="y")
		{
			new_y= getMouseY(e);
			var new_height= Math.max(20, new_y- editAreaLoader.resize["start_top"]);
			document.getElementById("edit_area_resize").style.height= new_height+"px";
		}
		if(allow=="both" || allow=="x")
		{
			new_x= getMouseX(e);
			var new_width= Math.max(20, new_x- editAreaLoader.resize["start_left"]);
			document.getElementById("edit_area_resize").style.width= new_width+"px";
		}
		//window.status="resize n_w: "+new_width+" new_h: "+new_height+ " new_y: "+new_y+" s_top: "+editAreaLoader.resize["start_top"];
		return false;
	};
	
	editAreaLoader.waiting_loading["resize_area.js"]= "loaded";
