var Process = require("geoscript/process").Process;

exports.process = new Process({
    title: "Intersection Test",
    description: "Determines whether a given geometry intersect target features.",
    inputs: {
        geometry: {
            type: "Geometry",
            title: "Input Geometry",
            description: "Input geometry that must intersect at least one target feature geometry."
        },
        features: {
            type: "FeatureCollection",
            title: "Target Features",
            description: "The feature collection to test for intersections."
        }
    },
    outputs: {
        intersects: {
            type: "Boolean",
            title: "Intersection Result",
            description: "The input geometry intersects at least one feature in the target feature set."
        },
        count: {
            type: "Integer",
            title: "Number of Intersections",
            description: "The number of target features intersected by the input geometry."
        }
    },
    run: function(inputs) {
        var geometry = inputs.geometry;
        var hits = 0;
        var target;
        for (var feature in inputs.features) {
            target = feature.geometry;
            if (target && geometry.intersects(target)) {
                ++hits;
            }
        }
        return {
            intersects: hits > 0,
            count: hits
        };
    }
});
