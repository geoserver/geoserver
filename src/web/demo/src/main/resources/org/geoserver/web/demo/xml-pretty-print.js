// https://www.npmjs.com/package/prettify-xml
//
// The MIT License (MIT)
// Copyright (c) 2016 Jonathan Werner
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

var stringTimesN = function stringTimesN(n, char) {
    return Array(n + 1).join(char);
};

// Adapted from https://gist.github.com/sente/1083506
function prettifyXml(xmlInput) {
    var options = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};
    var _options$indent = options.indent,
        indentOption = _options$indent === undefined ? 2 : _options$indent,
        _options$newline = options.newline,
        newlineOption = _options$newline === undefined ? _os.EOL : _options$newline;

    var indentString = stringTimesN(indentOption, ' ');

    var formatted = '';
    var regex = /(>)(<)(\/*)/g;
    var xml = xmlInput.replace(regex, '$1' + newlineOption + '$2$3');
    var pad = 0;
    xml.split(/\r?\n/).forEach(function (l) {
        var line = l.trim();

        var indent = 0;
        if (line.match(/.+<\/\w[^>]*>$/)) {
            indent = 0;
        } else if (line.match(/^<\/\w/)) {
            // Somehow istanbul doesn't see the else case as covered, although it is. Skip it.
            /* istanbul ignore else  */
            if (pad !== 0) {
                pad -= 1;
            }
        } else if (line.match(/^<\w([^>]*[^\/])?>.*$/)) {
            indent = 1;
        } else {
            indent = 0;
        }

        var padding = stringTimesN(pad, indentString);
        formatted += padding + line + newlineOption; // eslint-disable-line prefer-template
        pad += indent;
    });

    return formatted.trim();
}