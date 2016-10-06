// this is required to use wrapping/unwrapping in functions
var geoscript = require('geoscript');

exports.run = function(feature, args) {
    var geometry = args[0];
    var distance = args[1];
    return geometry.buffer(distance);
};
