$(document).ready(function() {
    
String.prototype.endsWith = function(pattern) {
    var d = this.length - pattern.length;
    return d >= 0 && this.lastIndexOf(pattern) === d;
};
    var watch = function(tag) {
        $(tag+' textarea').bind('input propertychange', function() {
            $(tag+' .srcpath span').removeClass( "active" );
            $(tag+' .destpath span').addClass( "active" );
            var text = $(tag+' .destpath span').text();
            if (text.substr(-1) !== "*") { //equiv to endswith "*"
                text += '*';
                $(tag+' .destpath span').text( text );
                
            }
        });
    };

    watch('#template_header');
    watch('#template_content');
    watch('#template_footer');

});



