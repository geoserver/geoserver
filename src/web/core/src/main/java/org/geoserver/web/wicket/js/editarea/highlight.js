	// change_to: "on" or "off"
	EditArea.prototype.change_highlight= function(change_to){
		if(this.settings["syntax"].length==0 && change_to==false){
			this.switchClassSticky($("highlight"), 'editAreaButtonDisabled', true);
			this.switchClassSticky($("reset_highlight"), 'editAreaButtonDisabled', true);
			return false;
		}
		
		if(this.do_highlight==change_to)
			return false;
	
			
		if(this.nav['isIE'])
			this.getIESelection();
		var pos_start= this.textarea.selectionStart;
		var pos_end= this.textarea.selectionEnd;
		
		if(this.do_highlight===true || change_to==false)
			this.disable_highlight();
		else
			this.enable_highlight();
		this.textarea.focus();
		this.textarea.selectionStart = pos_start;
		this.textarea.selectionEnd = pos_end;
		if(this.nav['isIE'])
			this.setIESelection();
				
	};
	
	EditArea.prototype.disable_highlight= function(displayOnly){		
		this.selection_field.innerHTML="";
		this.content_highlight.style.visibility="hidden";
		// replacing the node is far more faster than deleting it's content in firefox
		var new_Obj= this.content_highlight.cloneNode(false);
		new_Obj.innerHTML= "";			
		this.content_highlight.parentNode.insertBefore(new_Obj, this.content_highlight);
		this.content_highlight.parentNode.removeChild(this.content_highlight);	
		this.content_highlight= new_Obj;
		var old_class= parent.getAttribute(this.textarea,"class");
		if(old_class){
			var new_class= old_class.replace("hidden","");
			parent.setAttribute(this.textarea, "class", new_class);
		}
	
		this.textarea.style.backgroundColor="transparent";	// needed in order to see the bracket finders
		
		//var icon= document.getElementById("highlight");
		//setAttribute(icon, "class", getAttribute(icon, "class").replace(/ selected/g, "") );
		//this.restoreClass(icon);
		//this.switchClass(icon,'editAreaButtonNormal');
		this.switchClassSticky($("highlight"), 'editAreaButtonNormal', true);
		this.switchClassSticky($("reset_highlight"), 'editAreaButtonDisabled', true);
	
		this.do_highlight=false;
	
		this.switchClassSticky($("change_smooth_selection"), 'editAreaButtonSelected', true);
		if(typeof(this.smooth_selection_before_highlight)!="undefined" && this.smooth_selection_before_highlight===false){
			this.change_smooth_selection_mode(false);
		}
		
	//	this.textarea.style.backgroundColor="#FFFFFF";
	};

	EditArea.prototype.enable_highlight= function(){
		this.show_waiting_screen();
			
		this.content_highlight.style.visibility="visible";
		var new_class=parent.getAttribute(this.textarea,"class")+" hidden";
		parent.setAttribute(this.textarea, "class", new_class);
		
		if(this.nav['isIE'])
			this.textarea.style.backgroundColor="#FFFFFF";	// IE can't manage mouse click outside text range without this

		//var icon= document.getElementById("highlight");
		//setAttribute(icon, "class", getAttribute(icon, "class") + " selected");
		//this.switchClass(icon,'editAreaButtonSelected');
		//this.switchClassSticky($("highlight"), 'editAreaButtonNormal', false);
		this.switchClassSticky($("highlight"), 'editAreaButtonSelected', false);
		this.switchClassSticky($("reset_highlight"), 'editAreaButtonNormal', false);
		
		this.smooth_selection_before_highlight=this.smooth_selection;
		if(!this.smooth_selection)
			this.change_smooth_selection_mode(true);
		this.switchClassSticky($("change_smooth_selection"), 'editAreaButtonDisabled', true);
		
		
		this.do_highlight=true;
		this.resync_highlight();
					
		this.hide_waiting_screen();
		//area.onkeyup="";
		/*if(!displayOnly){
			this.do_highlight=true;
			this.reSync();
			if(this.state=="loaded")
				this.textarea.focus();
		}*/
	
	};
	
	
	EditArea.prototype.maj_highlight= function(infos){
		if(this.last_highlight_base_text==infos["full_text"] && this.resync_highlight!==true)
			return;
					
		//var infos= this.getSelectionInfos();
		if(infos["full_text"].indexOf("\r")!=-1)
			text_to_highlight= infos["full_text"].replace(/\r/g, "");
		else
			text_to_highlight= infos["full_text"];
		
		// for optimisation process
		var start_line_pb=-1;	
		var end_line_pb=-1;		
		
		var stay_begin="";	
		var stay_end="";
		
		
		var debug_opti="";
		
		// for speed mesure
		var date= new Date();
		var tps_start=date.getTime();		
		var tps_middle_opti=date.getTime();	
		
					
		//  OPTIMISATION: will search to update only changed lines
		if(this.reload_highlight===true){
			this.reload_highlight=false;
		}else if(text_to_highlight.length==0){
			text_to_highlight="\n ";
		}else{			
			var base_step= 200;
			var cpt= 0;
			var end= Math.min(text_to_highlight.length, this.last_text_to_highlight.length);
            var step= base_step;
            // find how many chars are similar at the begin of the text						
			while(cpt<end && step>=1){
                if(this.last_text_to_highlight.substr(cpt, step) == text_to_highlight.substr(cpt, step)){
                    cpt+= step;
                }else{
                    step= Math.floor(step/2);
                }
			}
			var pos_start_change=cpt;
			var line_start_change= text_to_highlight.substr(0, pos_start_change).split("\n").length -1;						
			
			cpt_last= this.last_text_to_highlight.length;
            cpt= text_to_highlight.length;
            step= base_step;			
            // find how many chars are similar at the end of the text						
			while(cpt>=0 && cpt_last>=0 && step>=1){
                if(this.last_text_to_highlight.substr(cpt_last-step, step) == text_to_highlight.substr(cpt-step, step)){
                    cpt-= step;
                    cpt_last-= step;
                }else{
                    step= Math.floor(step/2);
                }
			}
			//cpt_last=Math.max(0, cpt_last);
			var pos_new_end_change= cpt;
			var pos_last_end_change= cpt_last;
			if(pos_new_end_change<=pos_start_change){
				if(this.last_text_to_highlight.length < text_to_highlight.length){
					pos_new_end_change= pos_start_change + text_to_highlight.length - this.last_text_to_highlight.length;
					pos_last_end_change= pos_start_change;
				}else{
					pos_last_end_change= pos_start_change + this.last_text_to_highlight.length - text_to_highlight.length;
					pos_new_end_change= pos_start_change;
				}
			} 
			var change_new_text= text_to_highlight.substring(pos_start_change, pos_new_end_change);
			var change_last_text= this.last_text_to_highlight.substring(pos_start_change, pos_last_end_change);			            
			
			var line_new_end_change= text_to_highlight.substr(0, pos_new_end_change).split("\n").length -1;
			var line_last_end_change= this.last_text_to_highlight.substr(0, pos_last_end_change).split("\n").length -1;
			
			var change_new_text_line= text_to_highlight.split("\n").slice(line_start_change, line_new_end_change+1).join("\n");
			var change_last_text_line= this.last_text_to_highlight.split("\n").slice(line_start_change, line_last_end_change+1).join("\n");
		
			// check if it can only reparse the changed text
			var trace_new= this.get_syntax_trace(change_new_text_line);
			var trace_last= this.get_syntax_trace(change_last_text_line);
			if(trace_new == trace_last){
						
				date= new Date();		
				tps_middle_opti=date.getTime();	
			
				stay_begin= this.last_hightlighted_text.split("\n").slice(0, line_start_change).join("\n");
				if(line_start_change>0)
					stay_begin+= "\n";
				stay_end= this.last_hightlighted_text.split("\n").slice(line_last_end_change+1).join("\n");
				if(stay_end.length>0)
					stay_end= "\n"+stay_end;
	
	
				if(stay_begin.length==0 && pos_last_end_change==-1)
					change_new_text_line+="\n";
				text_to_highlight=change_new_text_line;
				
			}
			if(this.settings["debug"]){
				debug_opti= (trace_new == trace_last)?"Optimisation": "No optimisation";
				debug_opti+= " start: "+pos_start_change +"("+line_start_change+")";
				debug_opti+=" end_new: "+ pos_new_end_change+"("+line_new_end_change+")";
				debug_opti+=" end_last: "+ pos_last_end_change+"("+line_last_end_change+")";
				debug_opti+="\nchanged_text: "+change_new_text+" => trace: "+trace_new;
				debug_opti+="\nchanged_last_text: "+change_last_text+" => trace: "+trace_last;
				//debug_opti+= "\nchanged: "+ infos["full_text"].substring(pos_start_change, pos_new_end_change);
				debug_opti+= "\nchanged_line: "+change_new_text_line;
				debug_opti+= "\nlast_changed_line: "+change_last_text_line;
				debug_opti+="\nstay_begin: "+ stay_begin.slice(-200);
				debug_opti+="\nstay_end: "+ stay_end;
				//debug_opti="start: "+stay_begin_len+ "("+nb_line_start_unchanged+") end: "+ (stay_end_len)+ "("+(splited.length-nb_line_end_unchanged)+") ";
				//debug_opti+="changed: "+ text_to_highlight.substring(stay_begin_len, text_to_highlight.length-stay_end_len)+" \n";
				
				//debug_opti+="changed: "+ stay_begin.substr(stay_begin.length-200)+ "----------"+ text_to_highlight+"------------------"+ stay_end.substr(0,200) +"\n";
				debug_opti+="\n";
			}
	
			
			// END OPTIMISATION
		}
		date= new Date();
		tps_end_opti=date.getTime();	
				
		// apply highlight
		var updated_highlight= this.colorize_text(text_to_highlight);		
		// get the new highlight content
			
		date= new Date();
		tps2=date.getTime();
		//updated_highlight= "<div class='keywords'>"+updated_highlight+"</div>";
		var hightlighted_text= stay_begin + updated_highlight + stay_end;
		//this.previous_hightlight_content= tab_text.join("<br>");
		
		date= new Date();
		inner1=date.getTime();		
					
		// update the content of the highlight div by first updating a clone node (as there is no display in the same time for this node it's quite faster (5*))
		var new_Obj= this.content_highlight.cloneNode(false);
		if(this.nav['isIE'] || this.nav['isOpera'] )
			new_Obj.innerHTML= "<pre><span class='"+ this.settings["syntax"] +"'>" + hightlighted_text.replace("\n", "<br/>") + "</span></pre>";	
		else
			new_Obj.innerHTML= "<span class='"+ this.settings["syntax"] +"'>"+ hightlighted_text +"</span>";
	
		this.content_highlight.parentNode.replaceChild(new_Obj, this.content_highlight);
		
		this.content_highlight= new_Obj;
		if(infos["full_text"].indexOf("\r")!=-1)
			this.last_text_to_highlight= infos["full_text"].replace(/\r/g, "");
		else
			this.last_text_to_highlight= infos["full_text"];
		this.last_hightlighted_text= hightlighted_text;
		date= new Date();
		tps3=date.getTime();
	
		if(this.settings["debug"]){
			tot1=tps_end_opti-tps_start;
			tot_middle=tps_end_opti- tps_middle_opti;
			tot2=tps2-tps_end_opti;
			tps_join=inner1-tps2;			
			tps_td2=tps3-inner1;
			//lineNumber=tab_text.length;
			//this.debug.value+=" \nNB char: "+$("src").value.length+" Nb line: "+ lineNumber;
			this.debug.value= "Tps optimisation "+tot1+" (second part: "+tot_middle+") | tps reg exp: "+tot2+" | tps join: "+tps_join;
			this.debug.value+= " | tps update highlight content: "+tps_td2+"("+tps3+")\n";
			this.debug.value+=debug_opti;
		//	this.debug.value+= "highlight\n"+hightlighted_text;
		}
		
	};
	
	EditArea.prototype.resync_highlight= function(reload_now){
		this.reload_highlight=true;
		this.last_highlight_base_text="";
		this.focus();		
		if(reload_now)
			this.check_line_selection(false); 
	};	
