(function() {
 
    function handler(event) {
        // ensure we don't handle event when someone is typing in a form
        if ((event.target === document.documentElement) || (event.srcElement === document.body)) {
            // check for shift+p
            if (event.keyCode === 80 && event.shiftKey) {
                window.location = "?wicket:bookmarkablePage=:org.geoserver.web.demo.MapPreviewPage";
            }
        }
        
    }
    
    if (document.addEventListener) {
        document.addEventListener("keydown", handler, true);
    } else if (document.attachEvent) {
        // IE style
        document.attachEvent("onkeydown", handler);
    }
 
})();
