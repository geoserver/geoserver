$( document ).ready(function() {
    var fullscreen = false;
    var editorDefault = 300;
    var previewDefault = 300;

    var editorMargin = 180;
    var previewMargin = 224;

    var resizeStylePage = function() {
        var windowHeight = window.innerHeight
        var isFullscreen = $("#page").hasClass("fullscreen")

        var editorHeight = editorDefault;
        var previewHeight = previewDefault;

        if (isFullscreen) {
            document.getElementById('cm_editor_heigth').disabled=true;
            if (document.gsEditors) {
                //update the default to the old value
                editorDefault = document.gsEditors["editor"].getWrapperElement().offsetHeight;
            }
            document.gsEditors["editor"]

            editorHeight = Math.max(300, windowHeight - editorMargin);
            previewHeight = Math.max(300, windowHeight - previewMargin);

        } else {
            document.getElementById('cm_editor_heigth').disabled=false;
        }

        if (document.gsEditors) {
            var editor = document.gsEditors["editor"];
            editor.setSize("100%", editorHeight);
            editor.refresh();
        }

        if (window.olMap) {
            $("#olPreview").height(previewHeight)
            window.olMap.updateSize()
        }

    }
    window.resizeStylePage = resizeStylePage;


    $( "#fullscreen-link" ).click(function() {
        var img = $("#fullscreen-img");
        var page = $("#page");
        var pagePane = $("#page-pane");
        fullscreen = !fullscreen;
        if (!fullscreen) {
          document.documentElement.style.overflow = "unset";
        } else {
          document.documentElement.style.overflow = "hidden";
        }
        
        img.toggleClass("fullscreen-image-in")
        page.toggleClass("fullscreen");
        pagePane.toggleClass("page-pane-fullscreen");

        resizeStylePage();
    });

    window.addEventListener("resize", resizeStylePage);
});