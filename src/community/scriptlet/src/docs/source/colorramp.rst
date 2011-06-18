Generating a Dynamic Color Ramp
===============================
This example starts to get into using JavaScript to help Geoserver make better
maps.  In this tutorial, we'll demonstrate how to read in query parameters
(making the script truly dynamic) and use Rhino's `E4X
<https://developer.mozilla.org/en/E4X_Tutorial>`_ support for manipulating XML
documents.  In particular, we'll create an SLD with a color ramp from
user-provided parameters.

First, let's import a few classes and define a few functions to help with the
operations, converting from query strings to color values, etc::

    var format = java.lang.String.format;
    var MediaType = Packages.org.restlet.data.MediaType;
    var StringRepresentation = Packages.org.restlet.resource.StringRepresentation;

    function rgb(r, g, b) {
        r = java.lang.Integer.valueOf(255 * r);
        g = java.lang.Integer.valueOf(255 * g);
        b = java.lang.Integer.valueOf(255 * b);
        return format("#%02x%02x%02x", [r, g, b]);
    }

    function decodeHex(hex) {
        hex = hex.replaceFirst("^(#|0x)*", "");
        r = parseInt(hex.substring(0, 2), 16) / 255;
        g = parseInt(hex.substring(2, 4), 16) / 255;
        b = parseInt(hex.substring(4, 6), 16) / 255;
        return [r, g, b];
    }

So, rgb takes a Red, Green, Blue triple and creates a color hex string from it,
and decodeHex takes a color hex string and creates an array with the Red, Green,
and Blue values. Now, we can use those in a color ramp function::

    function ramp(from, to, min, max) {
        var ramps = [];

        from = decodeHex(from);
        var fR = from[0], fB = from[1], fG = from[2];
        to = decodeHex(to);
        var tR = to[0], tB = to[1], tG = to[2];

        for (var i = 0; i <= 1; i += 0.1) {
            var r = fR + (tR - fR) * i;
            var g = fG + (tG - fG) * i;
            var b = fB + (tB - fB) * i;
            ramps.push({
                color: rgb(r, g, b),
                dataValue: min + (max - min) * i
            });
        }

        return ramps;
    }

This just takes "from" and "to" as colors, and "min" and "max" as numbers, and
takes various weighted averages to create "stepped" colors partway through, a
simple equal-interval color ramp.  Now, we just need to get the value range and
the colors from the HTTP request::

    // Get the query parameters
    var form = request.getResourceRef().getQueryAsForm(); 
    // And any post parameters 
    form.addAll(request.getEntityAsForm());
    var from = form.getFirstValue("from");
    var to = form.getFirstValue("to");
    var min = parseFloat(form.getFirstValue("min"));
    var max = parseFloat(form.getFirstValue("max"));
    var attr = form.getFirstValue("attr");

To convert our color ramps from just a bunch of JavaScript objects to an SLD
that GeoServer can use to render a map, we can use E4X, which lets us embed XML
expressions directly in the code for easy manipulation::

    var sld = 
        <StyledLayerDescriptor version="1.0.0">
            <NamedLayer>
                <UserStyle>
                    <Title>Auto-generated Color Ramp</Title>
                    <FeatureTypeStyle/>
                </UserStyle>
            </NamedLayer>
        </StyledLayerDescriptor>

    var ramps = ramp(from, to, min, max);

    for (var i = 0, len = ramps.length - 1; i < len; i++) {
        var lower = ramps[i];
        var upper = ramps[i + 1];
        sld..FeatureTypeStyle[0].appendChild(
            <Rule>
                <Filter>
                    <PropertyIsBetween>
                        <PropertyName>{attr}</PropertyName>
                        <LowerBoundary>
                            <Literal>{lower.dataValue}</Literal>
                        </LowerBoundary>
                        <UpperBoundary>
                            <Literal>{upper.dataValue}</Literal>
                        </UpperBoundary>
                    </PropertyIsBetween>
                </Filter>
                <LineSymbolizer>
                    <Stroke>
                        <CssParameter name="stroke">
                            <Literal>{lower.color}</Literal>
                        </CssParameter>
                    </Stroke>
                </LineSymbolizer>
            </Rule>
        );
    }

And finally, don't forget to output the result after all that::

    response.setEntity(
        new StringRepresentation(sld.toString(), MediaType.TEXT_XML)
    );

.. seealso:: Mozilla's `E4X Tutorial
    <https://developer.mozilla.org/en/E4X_Tutorial>`_ for more information about
    E4X.
