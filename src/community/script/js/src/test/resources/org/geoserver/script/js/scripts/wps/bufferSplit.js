/**
 * This example uses process composition to create a new process that accesses
 * two existing processes.  The existing JTS:buffer and JTS:splitPolygon 
 * processes are used to create a new process that first buffers a geometry and
 * then splits it.  Since all geometries have a buffer method, it is contrived
 * to use an existing process for this, but the point is to demonstrate process
 * composition.
 * 
 * Get metadata about this process with the following:
 *     http://localhost:8080/geoserver/ows?service=wps&version=1.0.0&request=DescribeProcess&Identifier=js:bufferSplit
 */

var Process = require("geoscript/process").Process;

// get two existing processes for use later
var buffer = Process.get("geo:buffer");
var split = Process.get("geo:splitPolygon");

exports.process = new Process({
    title: "Buffer and Split",
    description: "Buffers a geometry and splits the resulting polygon.",
    inputs: {
        geom: {
            type: "Geometry",
            title:"Target Geometry",
            description: "The geometry to buffer."
        },
        distance: {
            type: "Number",
            title: "Buffer Distance",
            description: "The distance by which to buffer the target geometry."
        },
        line: {
            type: "LineString",
            title: "Splitter Line",
            description: "The line used for splitting the buffered geometry."
        }
    },
    outputs: {
        result: {
            type: "Geometry",
            title: "Result",
            description: "The buffered and split geometry."
        }
    },
    run: function(inputs) {
        var buffered = buffer.run({
            geom: inputs.geom, distance: inputs.distance
        });
        
        return split.run({polygon: buffered.result, line: inputs.line});
    }
});
