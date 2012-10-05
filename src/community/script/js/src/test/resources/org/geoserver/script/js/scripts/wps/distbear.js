var Process = require("geoscript/process").Process;
var {Feature, FeatureCollection, Schema} = require("geoscript/feature");

exports.process = new Process({

    // human readable title
    title: "Distance and Bearing",

    // describe process
    description: "Generates features with (cartesian) distance and bearing metrics given an existing feature collection and an origin.",

    // describe input parameters
    inputs: {
        origin: {
            type: "Point",
            title: "Origin",
            description: "The origin from which to calculate distance and bearing."
        },
        features: {
            type: "FeatureCollection",
            title: "Features",
            description: "The features to which distance and bearing should be calculated."
        }
    },

    // describe output parameters
    outputs: {
        result: {
            type: "FeatureCollection",
            title: "Resulting Features",
            description: "Features with calculated distance and bearing attributes."
        }
    },

    // provide a function that accepts inputs and returns outputs
    run: function(inputs) {
        var origin = inputs.origin;
        var geomField = inputs.features.schema.geometry;

        var schema = new Schema({
            name: "result",
            fields: [
                {name: "geometry", type: geomField.type, projection: geomField.projection},
                {name: "distance", type: "Double"},
                {name: "bearing", type: "Double"}
            ]
        });

        var collection = new FeatureCollection({
            features: function() {
                for (var feature in inputs.features) {

                    var point = feature.geometry.centroid;
                    var distance = origin.distance(point);
                    var bearing = (270 + Math.atan2(point.y - origin.y, point.x - origin.x) * 180 / Math.PI) % 360;

                    yield new Feature({
                        schema: schema,
                        properties: {
                            geometry: feature.geometry,
                            distance: distance,
                            bearing: bearing
                        }
                    });
                }
            }
        });

        return {result: collection};
    }

});
