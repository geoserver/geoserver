
GSModalWindow = {

    center: function() {
        var window = $('.modal-dialog')[0];
        var content = $('.w_content_container')[0];
        var scTop = 0;
        var height = this.getViewportHeight();
        var modalHeight = window.offsetHeight;
        if (modalHeight > height - 40) {
            content.style.height = (height - 40) + 'px';
            modalHeight = window.offsetHeight;
        }
        var top = (height / 2) - (modalHeight / 2) + scTop;
        if (top < 0) {
            top = 0;
        }
        window.style.top = top + 'px';
    },

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
    }
};
