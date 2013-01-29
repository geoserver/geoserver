/**
 * Get metadata about this process with the following:
 *     http://localhost:8080/geoserver/ows?service=wps&version=1.0.0&request=DescribeProcess&Identifier=js:buffer
 *     
 * Execute with the following for a full response document:
 *     http://localhost:8080/geoserver/ows?service=wps&version=1.0.0&request=Execute&Identifier=js:buffer&DataInputs=geom=POINT(1%201)@mimetype=application%2Fwkt;distance=2&ResponseDocument=result
 *
 * Or for just the raw data output:
 *     http://localhost:8080/geoserver/ows?service=wps&version=1.0.0&request=Execute&Identifier=js:buffer&DataInputs=geom=POINT(1%201)@mimetype=application%2Fwkt;distance=2&RawDataOutput=result
 */

var Process = require("geoscript/process").Process;

exports.process = new Process({
    title: "JavaScript Buffer Process",
    description: "Process that buffers a geometry.",
    inputs: {
        geom: {
            type: "Geometry",
            title:"Input Geometry",
            description: "The target geometry."
        },
        distance: {
            type: "Number",
            title: "Buffer Distance",
            description: "The distance by which to buffer the geometry."
        }
    },
    outputs: {
        result: {
            type: "Geometry",
            title: "Result",
            description: "The buffered geometry."
        }
    },
    run: function(inputs) {
        return {result: inputs.geom.buffer(inputs.distance)};
    }
});
