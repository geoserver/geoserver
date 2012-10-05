
var UTIL = require("geoscript/util");

/** api: (define)
 *  module = namespace
 *  class = Namespace
 */
var Namespace = UTIL.extend(Object, {

    /** api: config[alias]
     *  :class:`String`
     *  The namespace alias.
     */
    /** api: property[alias]
     *  :class:`String`
     *  The namespace alias.
     */

    /** api: config[uri]
     *  :class:`String`
     *  The namespace URI.
     */
    /** api: property[uri]
     *  :class:`String`
     *  The namespace URI.
     */

    /** api: constructor
     *  .. class:: Namespace
     *  
     *      :arg config: ``Object`` Configuration object.
     *
     *      Create a new namespace.
     */
    constructor: function Namespace(config) {
        if (!config || !("alias" in config) || !("uri" in config)) {
            throw new Error("Namespace config must include alias and uri.");
        }
        this.alias = config.alias;
        this.uri = config.uri;
    },

    /** private: method[toFullString]
     */
    toFullString: function() {
        return 'alias: "' + this.alias + '" uri: "' + this.uri + '"';
    }

});

exports.Namespace = Namespace;
