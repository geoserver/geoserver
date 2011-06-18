/**
 * ramp.js: Dynamically generate an SLD with a color ramp based on query 
 *     parameters.
 *
 * parameters: 
 *     attr: the attribute to reference in the SLD rules
 *     from: the color at the "low" end of the ramp, as a hex string
 *     to:   the color at the "high" end of the ramp, as a hex string
 *     min:  the minimum value of the range covered by the ramp, as a numeric 
 *           literal
 *     max:  the maximum value of the range covered by the ramp, as a numeric
 *           literal
 */

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

var form = request.getResourceRef().getQueryAsForm();
form.addAll(request.getEntityAsForm());
var from = form.getFirstValue("from");
var to = form.getFirstValue("to");
var min = parseFloat(form.getFirstValue("min"));
var max = parseFloat(form.getFirstValue("max"));
var attr = form.getFirstValue("attr");

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

response.setEntity(
    new StringRepresentation(sld.toString(), MediaType.TEXT_XML)
);
