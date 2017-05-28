$( document ).ready(function() {
    $( "#fullscreen-link" ).click(function() {
        var img = $("#fullscreen-img");
        var page = $("#page");
        var pagePane = $("#page-pane");
        document.documentElement.style.overflow = "hidden";
        img.toggleClass("fullscreen-image-in")
        page.toggleClass("fullscreen");
        pagePane.toggleClass("page-pane-fullscreen");
    });
});