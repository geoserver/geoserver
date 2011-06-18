	EditArea.prototype.focus = function() {
		this.textarea.focus();
		this.textareaFocused=true;
	};


	EditArea.prototype.check_line_selection= function(timer_checkup){
		//if(do_highlight==false){

		if(!editAreas[this.id])
			return false;
		
		//time=new Date;
		//t1=t2=t3= time.getTime();
		
		if(!this.smooth_selection && !this.do_highlight){
			//formatArea();
		}else if(this.textareaFocused && editAreas[this.id]["displayed"]==true && this.isResizing==false){
			infos= this.get_selection_infos();
		//	time=new Date;
		//	t2= time.getTime();
			
			//if(this.last_selection["line_start"] != infos["line_start"] || this.last_selection["line_nb"] != infos["line_nb"] || infos["full_text"] != this.last_selection["full_text"]){
			if(this.last_selection["line_start"] != infos["line_start"] || this.last_selection["line_nb"] != infos["line_nb"] || infos["full_text"] != this.last_selection["full_text"] || this.reload_highlight){
			// if selection change
				
				new_top=this.lineHeight * (infos["line_start"]-1);
				new_height=Math.max(0, this.lineHeight * infos["line_nb"]);
				new_width=Math.max(this.textarea.scrollWidth, this.container.clientWidth -50);
				
				this.selection_field.style.top=new_top+"px";	
				this.selection_field.style.width=new_width+"px";
				this.selection_field.style.height=new_height+"px";	
				$("cursor_pos").style.top=new_top+"px";	
		
				if(this.do_highlight==true){
					var curr_text=infos["full_text"].split("\n");
					var content="";
					//alert("length: "+curr_text.length+ " i: "+ Math.max(0,infos["line_start"]-1)+ " end: "+Math.min(curr_text.length, infos["line_start"]+infos["line_nb"]-1)+ " line: "+infos["line_start"]+" [0]: "+curr_text[0]+" [1]: "+curr_text[1]);
					var start=Math.max(0,infos["line_start"]-1);
					var end=Math.min(curr_text.length, infos["line_start"]+infos["line_nb"]-1);
					
					//curr_text[start]= curr_text[start].substr(0,infos["curr_pos"]-1) +"¤_overline_¤"+ curr_text[start].substr(infos["curr_pos"]-1);
					for(i=start; i< end; i++){
						content+= curr_text[i]+"\n";	
					}
					
					content= content.replace(/&/g,"&amp;");
					content= content.replace(/</g,"&lt;");
					content= content.replace(/>/g,"&gt;");
					
					if( this.nav['isIE'] || this.nav['isOpera'] )
						this.selection_field.innerHTML= "<pre>" + content.replace("\n", "<br/>") + "</pre>";	
					else
						this.selection_field.innerHTML=content;
						
					if(this.reload_highlight || (infos["full_text"] != this.last_text_to_highlight && (this.last_selection["line_start"]!=infos["line_start"] || this.show_line_colors || this.last_selection["line_nb"]!=infos["line_nb"] || this.last_selection["nb_line"]!=infos["nb_line"]) ) )
						this.maj_highlight(infos);
				}		
			}
		//	time=new Date;
		//	t3= time.getTime();
			
			// manage bracket finding
			if(infos["line_start"] != this.last_selection["line_start"] || infos["curr_pos"] != this.last_selection["curr_pos"] || infos["full_text"].length!=this.last_selection["full_text"].length || this.reload_highlight){
				// move _cursor_pos
				var selec_char= infos["curr_line"].charAt(infos["curr_pos"]-1);
				var no_real_move=true;
				if(infos["line_nb"]==1 && (this.assocBracket[selec_char] || this.revertAssocBracket[selec_char]) ){
					
					no_real_move=false;					
					//findEndBracket(infos["line_start"], infos["curr_pos"], selec_char);
					if(this.findEndBracket(infos, selec_char) === true){
						$("end_bracket").style.visibility="visible";
						$("cursor_pos").style.visibility="visible";
						$("cursor_pos").innerHTML= selec_char;
						$("end_bracket").innerHTML= (this.assocBracket[selec_char] || this.revertAssocBracket[selec_char]);
					}else{
						$("end_bracket").style.visibility="hidden";
						$("cursor_pos").style.visibility="hidden";
					}
				}else{
					$("cursor_pos").style.visibility="hidden";
					$("end_bracket").style.visibility="hidden";
				}
				//alert("move cursor");
				this.displayToCursorPosition("cursor_pos", infos["line_start"], infos["curr_pos"]-1, infos["curr_line"], no_real_move);
				if(infos["line_nb"]==1 && infos["line_start"]!=this.last_selection["line_start"])
					this.scroll_to_view();
			}
			this.last_selection=infos;
		}
	//	time=new Date;
	//	tend= time.getTime();
		//this.debug.value="tps total: "+ (tend-t1) + " tps get_infos: "+ (t2-t1)+ " tps jaune: "+ (t3-t2) +" tps cursor: "+ (tend-t3)+" "+typeof(infos);
		
		if(timer_checkup){
			//if(this.do_highlight==true)	//can slow down check speed when highlight mode is on
			setTimeout("editArea.check_line_selection(true)", this.check_line_selection_timer);
		}
	};


	EditArea.prototype.get_selection_infos= function(){
		if(this.nav['isIE'])
			this.getIESelection();
		start=this.textarea.selectionStart;
		end=this.textarea.selectionEnd;		
		
		if(this.last_selection["selectionStart"]==start && this.last_selection["selectionEnd"]==end && this.last_selection["full_text"]==this.textarea.value)
			return this.last_selection;
			
		if(this.tabulation!="\t" && this.textarea.value.indexOf("\t")!=-1) 
		{	// can append only after copy/paste 
			var len= this.textarea.value.length;
			this.textarea.value=this.replace_tab(this.textarea.value);
			start=end= start+(this.textarea.value.length-len);
			this.area_select(start, 0);
		}
		var selections=new Object();
		selections["selectionStart"]= start;
		selections["selectionEnd"]= end;		
		selections["full_text"]= this.textarea.value;
		selections["line_start"]=1;
		selections["line_nb"]=1;
		selections["curr_pos"]=0;
		selections["curr_line"]="";
		selections["indexOfCursor"]=0;
		selections["selec_direction"]= this.last_selection["selec_direction"];
		
		//return selections;	
		
		var splitTab=selections["full_text"].split("\n");
		var nbLine=Math.max(0, splitTab.length);		
		var nbChar=Math.max(0, selections["full_text"].length - (nbLine - 1));	// (remove \n caracters from the count)
		if(selections["full_text"].indexOf("\r")!=-1)
			nbChar= nbChar - (nbLine -1);		// (remove \r caracters from the count)
		selections["nb_line"]=nbLine;		
		selections["nb_char"]=nbChar;		
		if(start>0){
			var str=selections["full_text"].substr(0,start);
			selections["curr_pos"]= start - str.lastIndexOf("\n");
			selections["line_start"]=Math.max(1, str.split("\n").length);
		}else{
			selections["curr_pos"]=1;
		}
		if(end>start){
			selections["line_nb"]=selections["full_text"].substring(start,end).split("\n").length;
		}
		selections["indexOfCursor"]=this.textarea.selectionStart;		
		selections["curr_line"]=splitTab[Math.max(0,selections["line_start"]-1)];
		
		
		// determine in with direction the direction grow
		if(selections["selectionStart"]==this.last_selection["selectionStart"]){
			if(selections["selectionEnd"]>this.last_selection["selectionEnd"])
				selections["selec_direction"]= "down";
			else if(selections["selectionEnd"] == this.last_selection["selectionStart"])
				selections["selec_direction"]= this.last_selection["selec_direction"];
		}else if(selections["selectionStart"] == this.last_selection["selectionEnd"] && selections["selectionEnd"]>this.last_selection["selectionEnd"]){
			selections["selec_direction"]= "down";
		}else{
			selections["selec_direction"]= "up";
		}
			
		$("nbLine").innerHTML= nbLine;		
		$("nbChar").innerHTML= nbChar;		
		$("linePos").innerHTML=selections["line_start"];
		$("currPos").innerHTML=selections["curr_pos"];
		
		return selections;		
	};
	
	// set IE position in Firefox mode (textarea.selectionStart and textarea.selectionEnd)
	EditArea.prototype.getIESelection= function(){	
		try{
			var range = document.selection.createRange();
			var stored_range = range.duplicate();
			stored_range.moveToElementText( this.textarea );
			stored_range.setEndPoint( 'EndToEnd', range );
			if(stored_range.parentElement() !=this.textarea)
				return;
		
			// the range don't take care of empty lines in the end of the selection
			var scrollTop= this.result.scrollTop + document.body.scrollTop;
			
			var relative_top= range.offsetTop - parent.calculeOffsetTop(this.textarea) + scrollTop;
			
			var line_start = Math.round((relative_top / this.lineHeight) +1);
			
			var line_nb=Math.round(range.boundingHeight / this.lineHeight);
						
			var range_start=stored_range.text.length - range.text.length;
			var tab=this.textarea.value.substr(0, range_start).split("\n");			
			range_start+= (line_start - tab.length)*2;		// add missing empty lines to the selection
			this.textarea.selectionStart = range_start;
			
			var range_end=this.textarea.selectionStart + range.text.length;
			tab=this.textarea.value.substr(0, range_start + range.text.length).split("\n");			
			range_end+= (line_start + line_nb - 1 - tab.length)*2;
			
			this.textarea.selectionEnd = range_end;
		}
		catch(e){}
		/*this.textarea.selectionStart = 10;
		this.textarea.selectionEnd = 50;*/
	};
	
	// select the text for IE (and take care of \r caracters)
	EditArea.prototype.setIESelection= function(){
		var nbLineStart=this.textarea.value.substr(0, this.textarea.selectionStart).split("\n").length - 1;
		var nbLineEnd=this.textarea.value.substr(0, this.textarea.selectionEnd).split("\n").length - 1;
		var range = document.selection.createRange();
		range.moveToElementText( this.textarea );
		range.setEndPoint( 'EndToStart', range );
		
		range.moveStart('character', this.textarea.selectionStart - nbLineStart);
		range.moveEnd('character', this.textarea.selectionEnd - nbLineEnd - (this.textarea.selectionStart - nbLineStart)  );
		range.select();
	};
	
	EditArea.prototype.tab_selection= function(){
		if(this.is_tabbing)
			return;
		this.is_tabbing=true;
		//infos=getSelectionInfos();
		//if( document.selection ){
		if( this.nav['isIE'] )
			this.getIESelection();
		/* Insertion du code de formatage */
		var start = this.textarea.selectionStart;
		var end = this.textarea.selectionEnd;
		var insText = this.textarea.value.substring(start, end);
		
		/* Insert tabulation and ajust cursor position */
		var pos_start=start;
		var pos_end=end;
		if (insText.length == 0) {
			// if only one line selected
			this.textarea.value = this.textarea.value.substr(0, start) + this.tabulation + this.textarea.value.substr(end);
			pos_start = start + this.tabulation.length;
			pos_end=pos_start;
		} else {
			start= Math.max(0, this.textarea.value.substr(0, start).lastIndexOf("\n")+1);
			endText=this.textarea.value.substr(end);
			startText=this.textarea.value.substr(0, start);
			tmp= this.textarea.value.substring(start, end).split("\n");
			insText= this.tabulation+tmp.join("\n"+this.tabulation);
			this.textarea.value = startText + insText + endText;
			pos_start = start;
			pos_end= this.textarea.value.indexOf("\n", startText.length + insText.length);
			if(pos_end==-1)
				pos_end=this.textarea.value.length;
			//pos = start + repdeb.length + insText.length + ;
		}
		this.textarea.selectionStart = pos_start;
		this.textarea.selectionEnd = pos_end;
		
		//if( document.selection ){
		if(this.nav['isIE']){
			this.setIESelection();
			setTimeout("editArea.is_tabbing=false;", 100);	// IE can't accept to make 2 tabulation without a little break between both
		}else
			this.is_tabbing=false;	
		
  	};
	
	EditArea.prototype.invert_tab_selection= function(){
		if(this.is_tabbing)
			return;
		this.is_tabbing=true;
		//infos=getSelectionInfos();
		//if( document.selection ){
		if(this.nav['isIE'])
			this.getIESelection();
		
		var start = this.textarea.selectionStart;
		var end = this.textarea.selectionEnd;
		var insText = this.textarea.value.substring(start, end);
		
		/* Tab remove and cursor seleciton adjust */
		var pos_start=start;
		var pos_end=end;
		if (insText.length == 0) {
			if(this.textarea.value.substring(start-this.tabulation.length, start)==this.tabulation)
			{
				this.textarea.value = this.textarea.value.substr(0, start-this.tabulation.length) + this.textarea.value.substr(end);
				pos_start= Math.max(0, start-this.tabulation.length);
				pos_end=pos_start;
			}	
			/*
			this.textarea.value = this.textarea.value.substr(0, start) + this.tabulation + insText + this.textarea.value.substr(end);
			pos_start = start + this.tabulation.length;
			pos_end=pos_start;*/
		} else {
			start= this.textarea.value.substr(0, start).lastIndexOf("\n")+1;
			endText=this.textarea.value.substr(end);
			startText=this.textarea.value.substr(0, start);
			tmp= this.textarea.value.substring(start, end).split("\n");
			insText="";
			for(i=0; i<tmp.length; i++){				
				for(j=0; j<this.tab_nb_char; j++){
					if(tmp[i].charAt(0)=="\t"){
						tmp[i]=tmp[i].substr(1);
						j=this.tab_nb_char;
					}else if(tmp[i].charAt(0)==" ")
						tmp[i]=tmp[i].substr(1);
				}		
				insText+=tmp[i];
				if(i<tmp.length-1)
					insText+="\n";
			}
			//insText+="_";
			this.textarea.value = startText + insText + endText;
			pos_start = start;
			pos_end= this.textarea.value.indexOf("\n", startText.length + insText.length);
			if(pos_end==-1)
				pos_end=this.textarea.value.length;
			//pos = start + repdeb.length + insText.length + ;
		}
		this.textarea.selectionStart = pos_start;
		this.textarea.selectionEnd = pos_end;
		
		//if( document.selection ){
		if(this.nav['isIE']){
			// select the text for IE
			this.setIESelection();
			setTimeout("editArea.is_tabbing=false;", 100);	// IE can accept to make 2 tabulation without a little break between both
		}else
			this.is_tabbing=false;
  	};
	
	EditArea.prototype.press_enter= function(){		
		if(!this.smooth_selection)
			return false;
		if(this.nav['isIE'])
			this.getIESelection();
		var scrollTop= this.result.scrollTop;
		var scrollLeft= this.result.scrollLeft;
		var start=this.textarea.selectionStart;
		var end= this.textarea.selectionEnd;
		var start_last_line= Math.max(0 , this.textarea.value.substring(0, start).lastIndexOf("\n") + 1 );
		var begin_line= this.textarea.value.substring(start_last_line, start).replace(/^([ \t]*).*/gm, "$1");
		if(begin_line=="\n" || begin_line=="\r" || begin_line.length==0)
			return false;
			
		if(this.nav['isIE'] || this.nav['isOpera']){
			begin_line="\r\n"+ begin_line;
		}else{
			begin_line="\n"+ begin_line;
		}	
		//alert(start_last_line+" strat: "+start +"\n"+this.textarea.value.substring(start_last_line, start)+"\n_"+begin_line+"_")
		this.textarea.value= this.textarea.value.substring(0, start) + begin_line + this.textarea.value.substring(end);
		
		this.area_select(start+ begin_line.length ,0);
		// during this process IE scroll back to the top of the textarea
		if(this.nav['isIE']){
			this.result.scrollTop= scrollTop;
			this.result.scrollLeft= scrollLeft;
		}
		return true;
		
	};
	
	EditArea.prototype.findEndBracket= function(infos, bracket){
			
		var start=infos["indexOfCursor"];
		var normal_order=true;
		//curr_text=infos["full_text"].split("\n");
		if(this.assocBracket[bracket])
			endBracket=this.assocBracket[bracket];
		else if(this.revertAssocBracket[bracket]){
			endBracket=this.revertAssocBracket[bracket];
			normal_order=false;
		}	
		var end=-1;
		var nbBracketOpen=0;
		
		for(var i=start; i<infos["full_text"].length && i>=0; ){
			if(infos["full_text"].charAt(i)==endBracket){				
				nbBracketOpen--;
				if(nbBracketOpen<=0){
					//i=infos["full_text"].length;
					end=i;
					break;
				}
			}else if(infos["full_text"].charAt(i)==bracket)
				nbBracketOpen++;
			if(normal_order)
				i++;
			else
				i--;
		}
		
		//end=infos["full_text"].indexOf("}", start);
		if(end==-1)
			return false;	
		var endLastLine=infos["full_text"].substr(0, end).lastIndexOf("\n");			
		if(endLastLine==-1)
			line=1;
		else
			line= infos["full_text"].substr(0, endLastLine).split("\n").length + 1;
					
		var curPos= end - endLastLine;
		
		this.displayToCursorPosition("end_bracket", line, curPos, infos["full_text"].substring(endLastLine +1, end));
		return true;
	};
	
	EditArea.prototype.displayToCursorPosition= function(id, start_line, cur_pos, lineContent, no_real_move){
		var elem= $("test_font_size");
		var dest= $(id);
		var postLeft=0;
		elem.innerHTML="<pre><span id='test_font_size_inner'>"+lineContent.substr(0, cur_pos).replace(/&/g,"&amp;").replace(/</g,"&lt;")+"</span></pre>";
		posLeft= 45 + $('test_font_size_inner').offsetWidth;
		

		var posTop=this.lineHeight * (start_line-1);
		
		if( this.nav['isIE'] >= 8 )
			posTop--;
	
		if(no_real_move!=true){	// when the cursor is hidden no need to move him
			dest.style.top=posTop+"px";
			dest.style.left=posLeft+"px";		
		}
		// usefull for smarter scroll
		dest.cursor_top=posTop;
		dest.cursor_left=posLeft;
		
	//	$(id).style.marginLeft=posLeft+"px";
		
	};
	
	
	EditArea.prototype.area_select= function(start, length){
		this.textarea.focus();
		
		start= Math.max(0, Math.min(this.textarea.value.length, start));
		end= Math.max(start, Math.min(this.textarea.value.length, start+length));

		if(this.nav['isIE']){
			this.textarea.selectionStart = start;
			this.textarea.selectionEnd = end;		
			this.setIESelection();
		}else{
			if(this.nav['isOpera']){	// Opera bug when moving selection start and selection end
				/*this.textarea.selectionEnd = 1;	
				this.textarea.selectionStart = 0;			
				this.textarea.selectionEnd = 1;	
				this.textarea.selectionStart = 0;
				this.textarea.selectionEnd = 0;	
				this.textarea.selectionStart = 0;
				this.textarea.selectionEnd = 0;	
				this.textarea.selectionStart = 0;*/
				this.textarea.setSelectionRange(0, 0);
			}
			this.textarea.setSelectionRange(start, end);
		}
		this.check_line_selection();
	};
	
	
	EditArea.prototype.area_get_selection= function(){
		var text="";
		if( document.selection ){
			var range = document.selection.createRange();
			text=range.text;
		}else{
			text= this.textarea.value.substring(this.textarea.selectionStart, this.textarea.selectionEnd);
		}
		return text;			
	};
