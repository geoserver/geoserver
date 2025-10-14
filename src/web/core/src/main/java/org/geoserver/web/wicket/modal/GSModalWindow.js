//
// dragging:
//
// window.GSModalWindow_dragging - true if the user is actually dragging (mouse will be down)
//
// window.GSModalWindow_mousedragstart - {x:..,y:...} - the mouse location at the start of the drag.
//              + this is used to compute the "delta" between current mouse location and new mouse location
//
// window.GSModalWindow_dragstartloc - {x:..,y:...} - the start location of the window being dragged
//              + this is used with the mouse "delta" to position the window
//
// resizing:
//
// window.GSModalWindow_resizing - true if the user is actually resizing (mouse will be down)
// window.GSModalWindow_resize_mousestart - {x:..,y:...} - the mouse location at the start of the resize
// window.GSModalWindow_resize_startsize - {x:..,y:...} -  the start size of the window being resized
//
GSModalWindow = {

    /* centers the dialog pop-up on the viewport - based on the actual size of the popup */
    center: function() {
        var dialogWindow = $('.wicket-modal')[0];
        var content = $('.w_content_container')[0];
        var modalDialog = $('.modal-dialog')[0];

        var scTop = 0;
        var height = this.getViewportHeight();
        var width = this.getViewportWidth();

        var modalHeight = dialogWindow.getBoundingClientRect().height;
        var modalWidth = dialogWindow.getBoundingClientRect().width;

        if (modalHeight > height - 40) {
            content.style.height = (height - 40) + 'px';
            modalHeight = dialogWindow.offsetHeight;
        }
        if (modalWidth > width - 40) {
            content.style.width = (width - 40) + 'px';
            modalWidth = dialogWindow.offsetWidth;
        }
        var top = (height / 2) - (modalHeight / 2) + scTop;
        if (top < 0) {
            top = 0;
        }
        var left = (width / 2) - (modalWidth / 2)  ;
        if (left < 0) {
            left = 0;
        }
        modalDialog.style.left = left + 'px';
        modalDialog.style.top = top + 'px';
    },

    /* get width of the viewport - based on browser compatibility */
    getViewportWidth: function() {
        if (typeof(window.innerWidth) !== "undefined") {
            return window.innerWidth;
        }
        if (document.compatMode === 'CSS1Compat') {
            return document.documentElement.clientWidth;
        }
        if (document.body) {
            return document.body.clientWidth;
        }
        return undefined;
    },

    /* get height of the viewport - based on browser compatibility */
    getViewportHeight: function() {
        if (typeof(window.innerHeight) !== "undefined") {
            return window.innerHeight;
        }
        if (document.compatMode === 'CSS1Compat') {
            return document.documentElement.clientHeight;
        }
        if (document.body) {
            return document.body.clientHeight;
        }
        return undefined;
    },

    onbeforeunload: function() {
        return 'Reloading this page will cause the modal window to disappear.';
    },

    /* initializes a GSModalWindows.  Sets up drag/resize */
    initialize: function () {
        this.setupDrag();
        this.setupResize();

        // issue - some actions (like tabbing to another application) will make the browser lose
        // actions (like mouse up).  This will reset drag/resizing if that's detected.
        window.resetDragResize = function(e) {
            if (!window.GSModalWindow_dragging && !window.GSModalWindow_resizing) {
                return false;//no operation in progress
            }
            // no mouse buttons down?  We didn't get a mouse up events - cancel drag/resize.
            if (e.buttons === 0) {
                window.GSModalWindow_dragging = false;
                window.GSModalWindow_resizing = false;
                return true;
            }
            return false;
        }

    },



    // =================================== DRAG ====================================================

    /*sets up dragging functionality -
    * remove existing event listeners and add new ones.
    *
    * mousedown - on the area of interest (title bar)
    * mouseup   - on the window (in case mouse goes off the title bar during a drag)
    * mousemove - when mouse is moving.
    *
    *  mouseup/mousedown - sets a flag that we are dragging
    *  mousemove - if we are in a drag, then move the GS Modal Window
    *
    * */
    setupDrag: function () {
        let titleBar =  $('.modal-dialog .w_caption');
        titleBar.unbind('mousedown'); // clean up - we are going to re-attach
        $(window).off('mouseup',this.drag_window_mouseup); // clean up - we are going to re-attach
        $(window).off('mousemove',this.drag_window_mousemove); // clean up - we are going to re-attach

        //we find mousedown to the title bar so user can click on it.
        // we bind mouseup/mousemove to the window to handle if the user's mouse goes off the title bar
        // we can still stop dragging
        titleBar.bind('mousedown',this.drag_mousedown);
        $(window).bind('mouseup',this.drag_window_mouseup);
        $(window).bind('mousemove',this.drag_window_mousemove);

        window.GSModalWindow_dragging = false;

    },

    //handler for the mousemove
    // compute delta (difference between the start of the mousedown and the current mouse location).  ie.
    //               how far the mouse has been dragged.
    // change the dialog box top/left based on the delta and its GSModalWindow_dragstartloc (original location)
    drag_window_mousemove: function(e) {
        if (!window.GSModalWindow_dragging) {
           return; //not dragging
        }
        if (this.resetDragResize(e)) {
            return; // mouse isn't pressed - something weird happened.
        }

        // mouse move delta from original mouse-down location on screen
        var delta = {
            x:(e.clientX-window.GSModalWindow_mousedragstart.x),
            y:(e.clientY-window.GSModalWindow_mousedragstart.y)
        };

        //actually move the gs dialog window
        var element = $('.modal-dialog')[0];
        element.style.top = (window.GSModalWindow_dragstartloc.y + delta.y) +"px";
        element.style.left = (window.GSModalWindow_dragstartloc.x + delta.x) +"px";
    },

    //mouse pressed - set the "dragging" flag and record location of the mouse
    drag_mousedown: function(e) {
        window.GSModalWindow_mousedragstart={x:e.clientX,y:e.clientY};
        window.GSModalWindow_dragging = true;

        //location (top/left) of the element at the start of the drag
        var bounds =$('.modal-dialog')[0].getBoundingClientRect();
        window.GSModalWindow_dragstartloc = {x:bounds.x,y: bounds.y};
    },

    // stop dragging
    drag_window_mouseup: function(e) {
        window.GSModalWindow_dragging = false;
    },

    // =================================== RESIZE ====================================================


    // setup even listeners for the resizing (see setupDrag).
    setupResize: function () {
        let resizeDiv =  $('.modal-dialog .resize-icon');
        resizeDiv.unbind('mousedown'); // clean up - we are going to re-attach
        $(window).off('mouseup',this.resize_window_mouseup); // clean up - we are going to re-attach
        $(window).off('mousemove',this.resize_window_mousemove); // clean up - we are going to re-attach

        //we find mousedown to the title bar so user can click on it.
        // we bind mouseup/mousemove to the window to handle if the user's mouse goes off the resize icon
        // we can still stop resizing
        resizeDiv.bind('mousedown',this.resize_mousedown);
        $(window).bind('mouseup',this.resize_window_mouseup);
        $(window).bind('mousemove',this.resize_window_mousemove);

        window.GSModalWindow_resizing = false;
    },

    //1. record location of the mouse at the start of the resize
    //2. record the initial size of the gs dialog window
    //3. set the "resizing flag"
    resize_mousedown: function(e) {
        window.GSModalWindow_resizing = true;

        window.GSModalWindow_resize_mousestart={x:e.clientX,y:e.clientY};
        var boundingClientRect =$('.wicket-modal')[0].getBoundingClientRect();

        window.GSModalWindow_resize_startsize = {x:boundingClientRect.width,y:boundingClientRect.height};

        e.preventDefault();
    },

    // stop resizing
    resize_window_mouseup: function(e) {
        window.GSModalWindow_resizing = false;
    },

    // actually handle resizing
    // calculate the mouse move delta (from starting - mousedown - location)
    // change the size of the window (don't allow it to get too small)
    resize_window_mousemove: function(e) {
        if (!window.GSModalWindow_resizing) {
            return; //not resizing
        }

        if (this.resetDragResize(e)) {
            return; // mouse isn't pressed - something weird happened.
        }

        //calculate mouse change from start location (at the mouse down loc) to new location
        var element = $('.wicket-modal')[0];
        var delta = {
            x:(e.clientX-window.GSModalWindow_resize_mousestart.x),
            y:(e.clientY-window.GSModalWindow_resize_mousestart.y)
        };

        //new size = original size + mouse-move-delta
        var newwidth = (window.GSModalWindow_resize_startsize.x + delta.x);
        var newheight = (window.GSModalWindow_resize_startsize.y + delta.y);


        //prevent from getting too small
        var resize_minwidth = 200;
        var resize_minheight= 100;

        if (newwidth < resize_minwidth) {
            newwidth = resize_minwidth;
        }

        if (newheight < resize_minheight) {
            newheight = resize_minheight;
        }
        element.style.width = newwidth+"px";
        element.style.height = newheight+"px";
    }
};
