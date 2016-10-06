/**
 * Get metadata about this process with the following:
 *     http://localhost:8080/geoserver/ows?service=wps&version=1.0.0&request=DescribeProcess&Identifier=js:bufferedUnion
 */

var Process = require("geoscript/process").Process;

// this is contrived to use the union process here, but this tests process composition
var union = Process.get("geo:union");

exports.process = new Process({
    title: "Union & Buffer Process",
    description: "Process that unions a number of geometries and then buffers the result.",
    inputs: {
        geom: {
            type: "Geometry",
            title:"Input Geometries",
            description: "The geometries to union.",
            minOccurs: 2,
            maxOccurs: -1
        },
        distance: {
            type: "Number",
            title: "Buffer Distance",
            description: "The distance by which to buffer the result of the union."
        }
    },
    outputs: {
        result: {
            type: "Geometry",
            title: "Result",
            description: "The buffered union geometry."
        }
    },
    run: function(inputs) {
        var geom = union.run({geom: inputs.geom}).result;
        return {result: geom.buffer(inputs.distance)};
    }
});
