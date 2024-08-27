

//debugger;

//this is the code for the GSModalDialog for drag and resize


// ================================ DRAG =================================


//true means we are in a drag
GSModalDialog_dragging = false;

//start location (clientX and clientY) of the mousedown
GSModalDialog_mousedragstart={x:0,y:0};

//initial location (left/right) of the dialog.
GSModalDialog_dragstartloc = {x:0,y:0};

//handler for the mousemove
// compute delta (difference between the start of the mousedown and the current mouse location).  ie.
//               how far the mouse has been dragged.
// change the dialog box top/left based on the delta and its GSModalDialog_dragstartloc (original location)
function GSModalDialog_mousemove(e) {
    if (!GSModalDialog_dragging) {return;}
    //console.log('mousemove x='+e.clientX+', y='+e.clientY);
    var delta = {x:(e.clientX-GSModalDialog_mousedragstart.x),y:(e.clientY-GSModalDialog_mousedragstart.y)};
    //console.log("              mouse delta x="+delta.x+", delta y="+delta.y);
    var element =document.getElementById("GSModalDialogID");
    element.style.top = (GSModalDialog_dragstartloc.y + delta.y) +"px";
    element.style.left = (GSModalDialog_dragstartloc.x + delta.x) +"px";
}

//stop mouse drag
function GSModalDialog_mouseup(e) {
    //console.log('mouseup');
    GSModalDialog_dragging = false;
}

//start mouse drag
function GSModalDialog_mousedown(e) {
    //console.log('GSModalDialog_mousedown');
    GSModalDialog_dragging = true;
    GSModalDialog_mousedragstart={x:e.clientX,y:e.clientY};

    //location (top/left) of the element at the start of the drag
    var bounds =document.getElementById("GSModalDialogID").getBoundingClientRect();
    GSModalDialog_dragstartloc = {x:bounds.x,y: bounds.y};
    e.preventDefault();
}

// -----------------------------------------------------------------------------
//setup event handlers

//remove any existing
$(document).off('mousemove');// should be more specific incase other JS attaches
$(document).off('mouseup');// should be more specific incase other JS attaches

//add handlers
var dragDiv = $('#gsmodaldialog-header-drag');
if (dragDiv.length) {
    dragDiv.on('mousedown',GSModalDialog_mousedown)
    $(document).on('mouseup', null, {GSModalDialog:'GSModalDialog'},    GSModalDialog_mouseup )
    $(document).on('mousemove', null, {GSModalDialog:'GSModalDialog'},  GSModalDialog_mousemove  );
}


//========================================== RESIZE ===========================================

//debugger;

//prevent from getting too small
GSModalDialog_resize_minwidth = 200;
GSModalDialog_resize_minheight= 100;



//true means we are in a resize
GSModalDialog_resize = false;

//start location (clientX and clientY) of the mousedown
GSModalDialog_mousedragstart_resize = {x:0,y:0};

//initial size (width/height) of the dialog.
GSModalDialog_dragstartsize_resize = {x:0,y:0};

//initial location (left/right) of the dialog.
GSModalDialog_resizestartloc = {x:0,y:0};

function GSModalDialog_mousedown_resize(e) {
   // console.log("GSModalDialog_mousedown_resize");
    GSModalDialog_resize = true;

    GSModalDialog_mousedragstart_resize={x:e.clientX,y:e.clientY};
    var boundingClientRect =document.getElementById("GSModalDialogID").getBoundingClientRect();

    GSModalDialog_dragstartsize_resize = {x:boundingClientRect.width,y:boundingClientRect.height};

    GSModalDialog_resizestartloc = {x:boundingClientRect.x,y: boundingClientRect.y};
    e.preventDefault();
}

function GSModalDialog_mouseup_resize(e) {
    GSModalDialog_resize = false;
  //  console.log("GSModalDialog_mouseup_resize");
}

function GSModalDialog_mousemove_resize(e) {
  //  console.log("GSModalDialog_mousemove_resize");
    if (!GSModalDialog_resize) {return;}
    //console.log('mousemove x='+e.clientX+', y='+e.clientY);
    var delta = {x:(e.clientX-GSModalDialog_mousedragstart_resize.x),y:(e.clientY-GSModalDialog_mousedragstart_resize.y)};
   // console.log("              mouse delta x="+delta.x+", delta y="+delta.y);
    var element =document.getElementById("GSModalDialogID");

    var newwidth = (GSModalDialog_dragstartsize_resize.x + delta.x);
    var newheight = (GSModalDialog_dragstartsize_resize.y + delta.y);

    if (newwidth < GSModalDialog_resize_minwidth) {
        newwidth = GSModalDialog_resize_minwidth;
    }

    if (newheight < GSModalDialog_resize_minheight) {
        newheight = GSModalDialog_resize_minheight;
    }

    element.style.width = newwidth+"px";
    element.style.height = newheight+"px";

    //reset the top/left so they don't move (centering)
    element.style.top = (GSModalDialog_resizestartloc.y ) +"px";
    element.style.left = (GSModalDialog_resizestartloc.x ) +"px";
}

// -----------------------------------------------------------------------------
//setup event handlers

var resizeDiv = $('#gsmodaldialog-resizer-id');


if (resizeDiv.length) {
    $(resizeDiv).off('mousemove');

    //don't remove document listener - they were removed above (by drag).
    resizeDiv.on('mousedown',GSModalDialog_mousedown_resize)
    $(document).on('mouseup', null, {GSModalDialog:'GSModalDialog'},GSModalDialog_mouseup_resize)
    $(document).on('mousemove', null, {GSModalDialog:'GSModalDialog'},GSModalDialog_mousemove_resize)
}



//=============================

// var mainPopupContainer = $(".gsmodaldialog-content-container");
// if (mainPopupContainer.length) {
//     mainPopupContainer.focus();
//     mainPopupContainer.on('mousemove', null,  {GSModalDialog:'GSModalDialog'}, function (e) {
//         $(".gsmodaldialog-content-container").focus();
//         console.log("focused");
//     });
// }

