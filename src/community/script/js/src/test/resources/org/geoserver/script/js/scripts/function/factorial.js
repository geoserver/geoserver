function factorial(n) n > 0 && n * factorial(n - 1) || 1;

exports.run = function(value, args) {
    return factorial(value);
};
