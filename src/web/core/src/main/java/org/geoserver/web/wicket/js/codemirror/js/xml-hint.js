// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: https://codemirror.net/LICENSE

(function(mod) {
  if (typeof exports == "object" && typeof module == "object") // CommonJS
    mod(require("../../lib/codemirror"));
  else if (typeof define == "function" && define.amd) // AMD
    define(["../../lib/codemirror"], mod);
  else // Plain browser env
    mod(CodeMirror);
})(function(CodeMirror) {
  "use strict";
  
  var sld10tags = {
                    "!top": [
                      "StyledLayerDescriptor",
                      "Name",
                      "Title",
                      "Abstract",
                      "Localized",
                      "NamedLayer",
                      "NamedStyle",
                      "UserLayer",
                      "InlineFeature",
                      "RemoteOWS",
                      "Service",
                      "OnlineResource",
                      "LayerFeatureConstraints",
                      "FeatureTypeConstraint",
                      "FeatureTypeName",
                      "Extent",
                      "Value",
                      "UserStyle",
                      "IsDefault",
                      "FeatureTypeStyle",
                      "SemanticTypeIdentifier",
                      "Transformation",
                      "Rule",
                      "LegendGraphic",
                      "ElseFilter",
                      "MinScaleDenominator",
                      "MaxScaleDenominator",
                      "LineSymbolizer",
                      "Geometry",
                      "Stroke",
                      "CssParameter",
                      "GraphicFill",
                      "GraphicStroke",
                      "PolygonSymbolizer",
                      "Fill",
                      "PointSymbolizer",
                      "Graphic",
                      "Opacity",
                      "Size",
                      "Rotation",
                      "ExternalGraphic",
                      "Format",
                      "Mark",
                      "WellKnownName",
                      "TextSymbolizer",
                      "VendorOption",
                      "Priority",
                      "Label",
                      "Font",
                      "LabelPlacement",
                      "PointPlacement",
                      "AnchorPoint",
                      "AnchorPointX",
                      "AnchorPointY",
                      "Displacement",
                      "DisplacementX",
                      "DisplacementY",
                      "LinePlacement",
                      "PerpendicularOffset",
                      "Halo",
                      "Radius",
                      "RasterSymbolizer",
                      "ChannelSelection",
                      "RedChannel",
                      "GreenChannel",
                      "BlueChannel",
                      "GrayChannel",
                      "SourceChannelName",
                      "OverlapBehavior",
                      "LATEST_ON_TOP",
                      "EARLIEST_ON_TOP",
                      "AVERAGE",
                      "RANDOM",
                      "ColorMap",
                      "ColorMapEntry",
                      "ContrastEnhancement",
                      "Normalize",
                      "Histogram",
                      "Logarithmic",
                      "Exponential",
                      "GammaValue",
                      "ShadedRelief",
                      "BrightnessOnly",
                      "ReliefFactor",
                      "ImageOutline",
                      "cmns1:title",
                      "cmns1:resource",
                      "cmns1:locator",
                      "cmns1:arc",
                      "ogc:Add",
                      "ogc:Sub",
                      "ogc:Mul",
                      "ogc:Div",
                      "ogc:PropertyName",
                      "ogc:Function",
                      "ogc:Literal",
                      "ogc:FeatureId",
                      "ogc:Filter",
                      "ogc:PropertyIsEqualTo",
                      "ogc:PropertyIsNotEqualTo",
                      "ogc:PropertyIsLessThan",
                      "ogc:PropertyIsGreaterThan",
                      "ogc:PropertyIsLessThanOrEqualTo",
                      "ogc:PropertyIsGreaterThanOrEqualTo",
                      "ogc:PropertyIsLike",
                      "ogc:PropertyIsNull",
                      "ogc:PropertyIsBetween",
                      "ogc:Equals",
                      "ogc:Disjoint",
                      "ogc:Touches",
                      "ogc:Within",
                      "ogc:Overlaps",
                      "ogc:Crosses",
                      "ogc:Intersects",
                      "ogc:Contains",
                      "ogc:DWithin",
                      "ogc:Beyond",
                      "ogc:BBOX",
                      "ogc:And",
                      "ogc:Or",
                      "ogc:Not",
                      "gml:_Feature",
                      "gml:_FeatureCollection",
                      "gml:featureMember",
                      "gml:_geometryProperty",
                      "gml:geometryProperty",
                      "gml:boundedBy",
                      "gml:pointProperty",
                      "gml:polygonProperty",
                      "gml:lineStringProperty",
                      "gml:multiPointProperty",
                      "gml:multiLineStringProperty",
                      "gml:multiPolygonProperty",
                      "gml:multiGeometryProperty",
                      "gml:location",
                      "gml:centerOf",
                      "gml:position",
                      "gml:extentOf",
                      "gml:coverage",
                      "gml:edgeOf",
                      "gml:centerLineOf",
                      "gml:multiLocation",
                      "gml:multiCenterOf",
                      "gml:multiPosition",
                      "gml:multiCenterLineOf",
                      "gml:multiEdgeOf",
                      "gml:multiCoverage",
                      "gml:multiExtentOf",
                      "gml:description",
                      "gml:name",
                      "gml:_Geometry",
                      "gml:_GeometryCollection",
                      "gml:geometryMember",
                      "gml:pointMember",
                      "gml:lineStringMember",
                      "gml:polygonMember",
                      "gml:outerBoundaryIs",
                      "gml:innerBoundaryIs",
                      "gml:Point",
                      "gml:LineString",
                      "gml:LinearRing",
                      "gml:Polygon",
                      "gml:Box",
                      "gml:MultiGeometry",
                      "gml:MultiPoint",
                      "gml:MultiLineString",
                      "gml:MultiPolygon",
                      "gml:coord",
                      "gml:coordinates"
                    ],
                    "StyledLayerDescriptor": {
                      "attrs": {
                        "version": null
                      },
                      "children": [
                        "Name",
                        "Title",
                        "Abstract",
                        "NamedLayer",
                        "UserLayer"
                      ]
                    },
                    "Name": {},
                    "Title": {
                      "children": [
                        "Localized"
                      ]
                    },
                    "Abstract": {
                      "children": [
                        "Localized"
                      ]
                    },
                    "Localized": {
                      "attrs": {
                        "lang": null
                      }
                    },
                    "NamedLayer": {
                      "children": [
                        "Name",
                        "LayerFeatureConstraints",
                        "NamedStyle",
                        "UserStyle"
                      ]
                    },
                    "NamedStyle": {
                      "children": [
                        "Name"
                      ]
                    },
                    "UserLayer": {
                      "children": [
                        "Name",
                        "LayerFeatureConstraints",
                        "UserStyle",
                        "InlineFeature",
                        "RemoteOWS"
                      ]
                    },
                    "InlineFeature": {},
                    "RemoteOWS": {
                      "children": [
                        "Service",
                        "OnlineResource"
                      ]
                    },
                    "Service": {},
                    "OnlineResource": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ]
                      }
                    },
                    "LayerFeatureConstraints": {
                      "children": [
                        "FeatureTypeConstraint"
                      ]
                    },
                    "FeatureTypeConstraint": {
                      "children": [
                        "FeatureTypeName",
                        "ogc:Filter",
                        "Extent"
                      ]
                    },
                    "FeatureTypeName": {},
                    "Extent": {
                      "children": [
                        "Name",
                        "Value"
                      ]
                    },
                    "Value": {},
                    "UserStyle": {
                      "children": [
                        "Name",
                        "Title",
                        "Abstract",
                        "IsDefault",
                        "FeatureTypeStyle"
                      ]
                    },
                    "IsDefault": {},
                    "FeatureTypeStyle": {
                      "children": [
                        "Name",
                        "Title",
                        "Abstract",
                        "FeatureTypeName",
                        "SemanticTypeIdentifier",
                        "Transformation",
                        "Rule",
                        "VendorOption"
                      ]
                    },
                    "SemanticTypeIdentifier": {},
                    "Transformation": {
                      "children": [
                        "ogc:Function"
                      ]
                    },
                    "Rule": {
                      "children": [
                        "Name",
                        "Title",
                        "Abstract",
                        "LegendGraphic",
                        "MinScaleDenominator",
                        "MaxScaleDenominator",
                        "LineSymbolizer",
                        "PointSymbolizer",
                        "PolygonSymbolizer",
                        "TextSymbolizer",
                        "RasterSymbolizer",
                        "ogc:Filter",
                        "ElseFilter"
                      ]
                    },
                    "LegendGraphic": {
                      "children": [
                        "Graphic"
                      ]
                    },
                    "ElseFilter": {},
                    "MinScaleDenominator": {},
                    "MaxScaleDenominator": {},
                    "LineSymbolizer": {
                      "attrs": {
                        "uom": null
                      },
                      "children": [
                        "Geometry",
                        "Stroke",
                        "PerpendicularOffset",
                        "VendorOption"
                      ]
                    },
                    "Geometry": {
                      "children": [
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "Stroke": {
                      "children": [
                        "CssParameter",
                        "GraphicFill",
                        "GraphicStroke"
                      ]
                    },
                    "CssParameter": {
                      "attrs": {
                        "name": null
                      },
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "GraphicFill": {
                      "children": [
                        "Graphic"
                      ]
                    },
                    "GraphicStroke": {
                      "children": [
                        "Graphic"
                      ]
                    },
                    "PolygonSymbolizer": {
                      "attrs": {
                        "uom": null
                      },
                      "children": [
                        "Geometry",
                        "Fill",
                        "Stroke",
                        "VendorOption"
                      ]
                    },
                    "Fill": {
                      "children": [
                        "GraphicFill",
                        "CssParameter"
                      ]
                    },
                    "PointSymbolizer": {
                      "attrs": {
                        "uom": null
                      },
                      "children": [
                        "Geometry",
                        "Graphic",
                        "VendorOption"
                      ]
                    },
                    "Graphic": {
                      "children": [
                        "Opacity",
                        "Size",
                        "Rotation",
                        "AnchorPoint",
                        "Displacement",
                        "ExternalGraphic",
                        "Mark"
                      ]
                    },
                    "Opacity": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "Size": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "Rotation": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ExternalGraphic": {
                      "children": [
                        "OnlineResource",
                        "Format"
                      ]
                    },
                    "Format": {},
                    "Mark": {
                      "children": [
                        "WellKnownName",
                        "Fill",
                        "Stroke"
                      ]
                    },
                    "WellKnownName": {},
                    "TextSymbolizer": {
                      "attrs": {
                        "uom": null
                      },
                      "children": [
                        "Geometry",
                        "Label",
                        "Font",
                        "LabelPlacement",
                        "Halo",
                        "Fill",
                        "Graphic",
                        "Priority",
                        "VendorOption"
                      ]
                    },
                    "VendorOption": {
                      "attrs": {
                        "name": null
                      }
                    },
                    "Priority": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "Label": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "Font": {
                      "children": [
                        "CssParameter"
                      ]
                    },
                    "LabelPlacement": {
                      "children": [
                        "PointPlacement",
                        "LinePlacement"
                      ]
                    },
                    "PointPlacement": {
                      "children": [
                        "AnchorPoint",
                        "Displacement",
                        "Rotation"
                      ]
                    },
                    "AnchorPoint": {
                      "children": [
                        "AnchorPointX",
                        "AnchorPointY"
                      ]
                    },
                    "AnchorPointX": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "AnchorPointY": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "Displacement": {
                      "children": [
                        "DisplacementX",
                        "DisplacementY"
                      ]
                    },
                    "DisplacementX": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "DisplacementY": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "LinePlacement": {
                      "children": [
                        "PerpendicularOffset"
                      ]
                    },
                    "PerpendicularOffset": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "Halo": {
                      "children": [
                        "Radius",
                        "Fill"
                      ]
                    },
                    "Radius": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "RasterSymbolizer": {
                      "attrs": {
                        "uom": null
                      },
                      "children": [
                        "Geometry",
                        "Opacity",
                        "ChannelSelection",
                        "OverlapBehavior",
                        "ColorMap",
                        "ContrastEnhancement",
                        "ShadedRelief",
                        "ImageOutline",
                        "VendorOption"
                      ]
                    },
                    "ChannelSelection": {
                      "children": [
                        "GrayChannel",
                        "RedChannel",
                        "GreenChannel",
                        "BlueChannel"
                      ]
                    },
                    "RedChannel": {
                      "children": [
                        "SourceChannelName",
                        "ContrastEnhancement"
                      ]
                    },
                    "GreenChannel": {
                      "children": [
                        "SourceChannelName",
                        "ContrastEnhancement"
                      ]
                    },
                    "BlueChannel": {
                      "children": [
                        "SourceChannelName",
                        "ContrastEnhancement"
                      ]
                    },
                    "GrayChannel": {
                      "children": [
                        "SourceChannelName",
                        "ContrastEnhancement"
                      ]
                    },
                    "SourceChannelName": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "OverlapBehavior": {
                      "children": [
                        "LATEST_ON_TOP",
                        "EARLIEST_ON_TOP",
                        "AVERAGE",
                        "RANDOM"
                      ]
                    },
                    "LATEST_ON_TOP": {},
                    "EARLIEST_ON_TOP": {},
                    "AVERAGE": {},
                    "RANDOM": {},
                    "ColorMap": {
                      "attrs": {
                        "type": null,
                        "extended": null
                      },
                      "children": [
                        "ColorMapEntry"
                      ]
                    },
                    "ColorMapEntry": {
                      "attrs": {
                        "color": null,
                        "opacity": null,
                        "quantity": null,
                        "label": null
                      }
                    },
                    "ContrastEnhancement": {
                      "children": [
                        "GammaValue",
                        "Normalize",
                        "Logarithmic",
                        "Exponential",
                        "Histogram"
                      ]
                    },
                    "Normalize": {
                      "children": [
                        "VendorOption"
                      ]
                    },
                    "VendorOption": {},
                    "Logarithmic": {
                      "children": [
                        "VendorOption"
                      ]
                    },
                    "Exponential": {
                      "children": [
                        "VendorOption"
                      ]
                    },
                    "Histogram": {
                      "children": [
                        "VendorOption"
                      ]
                    },
                    "Normalize": {},
                    "Histogram": {},
                    "Logarithmic": {},
                    "Exponential": {},
                    "GammaValue": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ShadedRelief": {
                      "children": [
                        "BrightnessOnly",
                        "ReliefFactor"
                      ]
                    },
                    "BrightnessOnly": {},
                    "ReliefFactor": {},
                    "ImageOutline": {
                      "children": [
                        "LineSymbolizer",
                        "PolygonSymbolizer"
                      ]
                    },
                    "cmns1:title": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "lang": null
                      }
                    },
                    "cmns1:resource": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "role": null,
                        "title": null,
                        "label": null
                      }
                    },
                    "cmns1:locator": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "title": null,
                        "label": null
                      },
                      "children": [
                        "cmns1:title"
                      ]
                    },
                    "cmns1:arc": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "from": null,
                        "to": null
                      },
                      "children": [
                        "cmns1:title"
                      ]
                    },
                    "ogc:Add": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:Sub": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:Mul": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:Div": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:PropertyName": {},
                    "ogc:Function": {
                      "attrs": {
                        "name": null
                      },
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:Literal": {},
                    "ogc:FeatureId": {
                      "attrs": {
                        "fid": null
                      }
                    },
                    "ogc:Filter": {
                      "children": [
                      "ogc:PropertyIsEqualTo",
                      "ogc:PropertyIsNotEqualTo",
                      "ogc:PropertyIsLessThan",
                      "ogc:PropertyIsGreaterThan",
                      "ogc:PropertyIsLessThanOrEqualTo",
                      "ogc:PropertyIsGreaterThanOrEqualTo",
                      "ogc:PropertyIsLike",
                      "ogc:PropertyIsNull",
                      "ogc:PropertyIsBetween",
                      "ogc:Equals",
                      "ogc:Disjoint",
                      "ogc:Touches",
                      "ogc:Within",
                      "ogc:Overlaps",
                      "ogc:Crosses",
                      "ogc:Intersects",
                      "ogc:Contains",
                      "ogc:DWithin",
                      "ogc:Beyond",
                      "ogc:BBOX",
                      "ogc:And",
                      "ogc:Or",
                      "ogc:Not",
                  	"ogc:FeatureId"
                      ]
                    },
                    "ogc:comparisonOps": {},
                    "ogc:PropertyIsEqualTo": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:PropertyIsNotEqualTo": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:PropertyIsLessThan": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:PropertyIsGreaterThan": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:PropertyIsLessThanOrEqualTo": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:PropertyIsGreaterThanOrEqualTo": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:PropertyIsLike": {
                      "attrs": {
                        "wildCard": null,
                        "singleChar": null,
                        "escape": null
                      },
                      "children": [
                        "ogc:PropertyName",
                        "ogc:Literal"
                      ]
                    },
                    "ogc:PropertyIsNull": {
                      "children": [
                        "ogc:PropertyName",
                        "ogc:Literal"
                      ]
                    },
                    "ogc:PropertyIsBetween": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                        "ogc:LowerBoundary",
                        "ogc:UpperBoundary"
                      ]
                    },
                    "ogc:LowerBoundary": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:UpperBoundary": {
                      "children": [
                          "ogc:Add",
                          "ogc:Sub",
                          "ogc:Mul",
                          "ogc:Div",
                          "ogc:PropertyName",
                          "ogc:Function",
                          "ogc:Literal",
                      ]
                    },
                    "ogc:Equals": {
                      "children": [
                        "ogc:PropertyName",
                        "gml:_Geometry",
                        "gml:Box"
                      ]
                    },
                    "ogc:Disjoint": {
                      "children": [
                        "ogc:PropertyName",
                        "gml:_Geometry",
                        "gml:Box"
                      ]
                    },
                    "ogc:Touches": {
                      "children": [
                        "ogc:PropertyName",
                        "gml:_Geometry",
                        "gml:Box"
                      ]
                    },
                    "ogc:Within": {
                      "children": [
                        "ogc:PropertyName",
                        "gml:_Geometry",
                        "gml:Box"
                      ]
                    },
                    "ogc:Overlaps": {
                      "children": [
                        "ogc:PropertyName",
                        "gml:_Geometry",
                        "gml:Box"
                      ]
                    },
                    "ogc:Crosses": {
                      "children": [
                        "ogc:PropertyName",
                        "gml:_Geometry",
                        "gml:Box"
                      ]
                    },
                    "ogc:Intersects": {
                      "children": [
                        "ogc:PropertyName",
                        "gml:_Geometry",
                        "gml:Box"
                      ]
                    },
                    "ogc:Contains": {
                      "children": [
                        "ogc:PropertyName",
                        "gml:_Geometry",
                        "gml:Box"
                      ]
                    },
                    "ogc:DWithin": {
                      "children": [
                        "ogc:PropertyName",
                        "gml:_Geometry",
                        "ogc:Distance"
                      ]
                    },
                    "ogc:Distance": {
                      "attrs": {
                        "units": null
                      }
                    },
                    "ogc:Beyond": {
                      "children": [
                        "ogc:PropertyName",
                        "gml:_Geometry",
                        "ogc:Distance"
                      ]
                    },
                    "ogc:BBOX": {
                      "children": [
                        "ogc:PropertyName",
                        "gml:Box"
                      ]
                    },
                    "ogc:logicOps": {},
                    "ogc:And": {
                      "children": [
                      "ogc:PropertyIsEqualTo",
                      "ogc:PropertyIsNotEqualTo",
                      "ogc:PropertyIsLessThan",
                      "ogc:PropertyIsGreaterThan",
                      "ogc:PropertyIsLessThanOrEqualTo",
                      "ogc:PropertyIsGreaterThanOrEqualTo",
                      "ogc:PropertyIsLike",
                      "ogc:PropertyIsNull",
                      "ogc:PropertyIsBetween",
                      "ogc:Equals",
                      "ogc:Disjoint",
                      "ogc:Touches",
                      "ogc:Within",
                      "ogc:Overlaps",
                      "ogc:Crosses",
                      "ogc:Intersects",
                      "ogc:Contains",
                      "ogc:DWithin",
                      "ogc:Beyond",
                      "ogc:BBOX",
                      "ogc:And",
                      "ogc:Or",
                      "ogc:Not"   
                  	]
                    },
                    "ogc:Or": {
                      "children": [
                      "ogc:PropertyIsEqualTo",
                      "ogc:PropertyIsNotEqualTo",
                      "ogc:PropertyIsLessThan",
                      "ogc:PropertyIsGreaterThan",
                      "ogc:PropertyIsLessThanOrEqualTo",
                      "ogc:PropertyIsGreaterThanOrEqualTo",
                      "ogc:PropertyIsLike",
                      "ogc:PropertyIsNull",
                      "ogc:PropertyIsBetween",
                      "ogc:Equals",
                      "ogc:Disjoint",
                      "ogc:Touches",
                      "ogc:Within",
                      "ogc:Overlaps",
                      "ogc:Crosses",
                      "ogc:Intersects",
                      "ogc:Contains",
                      "ogc:DWithin",
                      "ogc:Beyond",
                      "ogc:BBOX",
                      "ogc:And",
                      "ogc:Or",
                      "ogc:Not"
                      ]
                    },
                    "ogc:Not": {
                      "children": [
                      "ogc:PropertyIsEqualTo",
                      "ogc:PropertyIsNotEqualTo",
                      "ogc:PropertyIsLessThan",
                      "ogc:PropertyIsGreaterThan",
                      "ogc:PropertyIsLessThanOrEqualTo",
                      "ogc:PropertyIsGreaterThanOrEqualTo",
                      "ogc:PropertyIsLike",
                      "ogc:PropertyIsNull",
                      "ogc:PropertyIsBetween",
                      "ogc:Equals",
                      "ogc:Disjoint",
                      "ogc:Touches",
                      "ogc:Within",
                      "ogc:Overlaps",
                      "ogc:Crosses",
                      "ogc:Intersects",
                      "ogc:Contains",
                      "ogc:DWithin",
                      "ogc:Beyond",
                      "ogc:BBOX",
                      "ogc:And",
                      "ogc:Or",
                      "ogc:Not"
                      ]
                    },
                    "gml:_Feature": {
                      "attrs": {
                        "fid": null
                      },
                      "children": [
                        "gml:description",
                        "gml:name",
                        "gml:boundedBy"
                      ]
                    },
                    "gml:_FeatureCollection": {
                      "attrs": {
                        "fid": null
                      },
                      "children": [
                        "gml:description",
                        "gml:name",
                        "gml:boundedBy",
                        "gml:featureMember"
                      ]
                    },
                    "gml:featureMember": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:_Feature"
                      ]
                    },
                    "gml:_geometryProperty": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:_Geometry"
                      ]
                    },
                    "gml:geometryProperty": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:_Geometry"
                      ]
                    },
                    "gml:boundedBy": {
                      "children": [
                        "gml:Box",
                        "gml:null"
                      ]
                    },
                    "gml:null": {},
                    "gml:pointProperty": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:Point"
                      ]
                    },
                    "gml:polygonProperty": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:Polygon"
                      ]
                    },
                    "gml:lineStringProperty": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:LineString"
                      ]
                    },
                    "gml:multiPointProperty": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:MultiPoint"
                      ]
                    },
                    "gml:multiLineStringProperty": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:MultiLineString"
                      ]
                    },
                    "gml:multiPolygonProperty": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:MultiPolygon"
                      ]
                    },
                    "gml:multiGeometryProperty": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:MultiGeometry"
                      ]
                    },
                    "gml:location": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:Point"
                      ]
                    },
                    "gml:centerOf": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:Point"
                      ]
                    },
                    "gml:position": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:Point"
                      ]
                    },
                    "gml:extentOf": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:Polygon"
                      ]
                    },
                    "gml:coverage": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:Polygon"
                      ]
                    },
                    "gml:edgeOf": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:LineString"
                      ]
                    },
                    "gml:centerLineOf": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:LineString"
                      ]
                    },
                    "gml:multiLocation": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:MultiPoint"
                      ]
                    },
                    "gml:multiCenterOf": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:MultiPoint"
                      ]
                    },
                    "gml:multiPosition": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:MultiPoint"
                      ]
                    },
                    "gml:multiCenterLineOf": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:MultiLineString"
                      ]
                    },
                    "gml:multiEdgeOf": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:MultiLineString"
                      ]
                    },
                    "gml:multiCoverage": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:MultiPolygon"
                      ]
                    },
                    "gml:multiExtentOf": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:MultiPolygon"
                      ]
                    },
                    "gml:description": {},
                    "gml:name": {},
                    "gml:_Geometry": {
                      "attrs": {
                        "gid": null,
                        "srsName": null
                      }
                    },
                    "gml:_GeometryCollection": {
                      "attrs": {
                        "gid": null,
                        "srsName": null
                      },
                      "children": [
                        "gml:geometryMember"
                      ]
                    },
                    "gml:geometryMember": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:_Geometry"
                      ]
                    },
                    "gml:pointMember": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:Point"
                      ]
                    },
                    "gml:lineStringMember": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:LineString"
                      ]
                    },
                    "gml:polygonMember": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:Polygon"
                      ]
                    },
                    "gml:outerBoundaryIs": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:LinearRing"
                      ]
                    },
                    "gml:innerBoundaryIs": {
                      "attrs": {
                        "type": [
                          "simple",
                          "extended",
                          "title",
                          "resource",
                          "locator",
                          "arc"
                        ],
                        "href": null,
                        "role": null,
                        "arcrole": null,
                        "title": null,
                        "show": [
                          "new",
                          "replace",
                          "embed",
                          "other",
                          "none"
                        ],
                        "actuate": [
                          "onLoad",
                          "onRequest",
                          "other",
                          "none"
                        ],
                        "remoteSchema": null
                      },
                      "children": [
                        "gml:LinearRing"
                      ]
                    },
                    "gml:Point": {
                      "attrs": {
                        "gid": null,
                        "srsName": null
                      },
                      "children": [
                        "gml:coord",
                        "gml:coordinates"
                      ]
                    },
                    "gml:LineString": {
                      "attrs": {
                        "gid": null,
                        "srsName": null
                      },
                      "children": [
                        "gml:coord",
                        "gml:coordinates"
                      ]
                    },
                    "gml:LinearRing": {
                      "attrs": {
                        "gid": null,
                        "srsName": null
                      },
                      "children": [
                        "gml:coord",
                        "gml:coordinates"
                      ]
                    },
                    "gml:Polygon": {
                      "attrs": {
                        "gid": null,
                        "srsName": null
                      },
                      "children": [
                        "gml:outerBoundaryIs",
                        "gml:innerBoundaryIs"
                      ]
                    },
                    "gml:Box": {
                      "attrs": {
                        "gid": null,
                        "srsName": null
                      },
                      "children": [
                        "gml:coord",
                        "gml:coordinates"
                      ]
                    },
                    "gml:MultiGeometry": {
                      "attrs": {
                        "gid": null,
                        "srsName": null
                      },
                      "children": [
                        "gml:geometryMember"
                      ]
                    },
                    "gml:MultiPoint": {
                      "attrs": {
                        "gid": null,
                        "srsName": null
                      },
                      "children": [
                        "gml:pointMember"
                      ]
                    },
                    "gml:MultiLineString": {
                      "attrs": {
                        "gid": null,
                        "srsName": null
                      },
                      "children": [
                        "gml:lineStringMember"
                      ]
                    },
                    "gml:MultiPolygon": {
                      "attrs": {
                        "gid": null,
                        "srsName": null
                      },
                      "children": [
                        "gml:polygonMember"
                      ]
                    },
                    "gml:coord": {
                      "children": [
                        "gml:X",
                        "gml:Y",
                        "gml:Z"
                      ]
                    },
                    "gml:X": {},
                    "gml:Y": {},
                    "gml:Z": {},
                    "gml:coordinates": {
                      "attrs": {
                        "decimal": null,
                        "cs": null,
                        "ts": null
                      }
                    }
                  };

  var Pos = CodeMirror.Pos;

  function getHints(cm, options) {
    var tags = options && options.schemaInfo;
    var quote = (options && options.quoteChar) || '"';
    if (!tags) return;
    var cur = cm.getCursor(), token = cm.getTokenAt(cur);
    if (token.end > cur.ch) {
      token.end = cur.ch;
      token.string = token.string.slice(0, cur.ch - token.start);
    }
    var inner = CodeMirror.innerMode(cm.getMode(), token.state);
    if (inner.mode.name != "xml") return;
    var result = [], replaceToken = false, prefix;
    var tag = /\btag\b/.test(token.type) && !/>$/.test(token.string);
    var tagName = tag && /^\w/.test(token.string), tagStart;

    if (tagName) {
      var before = cm.getLine(cur.line).slice(Math.max(0, token.start - 2), token.start);
      var tagType = /<\/$/.test(before) ? "close" : /<$/.test(before) ? "open" : null;
      if (tagType) tagStart = token.start - (tagType == "close" ? 2 : 1);
    } else if (tag && token.string == "<") {
      tagType = "open";
    } else if (tag && token.string == "</") {
      tagType = "close";
    }

    if (!tag && !inner.state.tagName || tagType) {
      if (tagName)
        prefix = token.string;
      replaceToken = tagType;
      var cx = inner.state.context, curTag = cx && tags[cx.tagName];
      var childList = cx ? curTag && curTag.children : tags["!top"];
      if (childList && tagType != "close") {
        for (var i = 0; i < childList.length; ++i) if (!prefix || childList[i].lastIndexOf(prefix, 0) == 0)
          result.push("<" + childList[i]);
      } else if (tagType != "close") {
        for (var name in tags)
          if (tags.hasOwnProperty(name) && name != "!top" && name != "!attrs" && (!prefix || name.lastIndexOf(prefix, 0) == 0))
            result.push("<" + name);
      }
      if (cx && (!prefix || tagType == "close" && cx.tagName.lastIndexOf(prefix, 0) == 0))
        result.push("</" + cx.tagName + ">");
    } else {
      // Attribute completion
      var curTag = tags[inner.state.tagName], attrs = curTag && curTag.attrs;
      var globalAttrs = tags["!attrs"];
      if (!attrs && !globalAttrs) return;
      if (!attrs) {
        attrs = globalAttrs;
      } else if (globalAttrs) { // Combine tag-local and global attributes
        var set = {};
        for (var nm in globalAttrs) if (globalAttrs.hasOwnProperty(nm)) set[nm] = globalAttrs[nm];
        for (var nm in attrs) if (attrs.hasOwnProperty(nm)) set[nm] = attrs[nm];
        attrs = set;
      }
      if (token.type == "string" || token.string == "=") { // A value
        var before = cm.getRange(Pos(cur.line, Math.max(0, cur.ch - 60)),
                                 Pos(cur.line, token.type == "string" ? token.start : token.end));
        var atName = before.match(/([^\s\u00a0=<>\"\']+)=$/), atValues;
        if (!atName || !attrs.hasOwnProperty(atName[1]) || !(atValues = attrs[atName[1]])) return;
        if (typeof atValues == 'function') atValues = atValues.call(this, cm); // Functions can be used to supply values for autocomplete widget
        if (token.type == "string") {
          prefix = token.string;
          var n = 0;
          if (/['"]/.test(token.string.charAt(0))) {
            quote = token.string.charAt(0);
            prefix = token.string.slice(1);
            n++;
          }
          var len = token.string.length;
          if (/['"]/.test(token.string.charAt(len - 1))) {
            quote = token.string.charAt(len - 1);
            prefix = token.string.substr(n, len - 2);
          }
          replaceToken = true;
        }
        for (var i = 0; i < atValues.length; ++i) if (!prefix || atValues[i].lastIndexOf(prefix, 0) == 0)
          result.push(quote + atValues[i] + quote);
      } else { // An attribute name
        if (token.type == "attribute") {
          prefix = token.string;
          replaceToken = true;
        }
        for (var attr in attrs) if (attrs.hasOwnProperty(attr) && (!prefix || attr.lastIndexOf(prefix, 0) == 0))
          result.push(attr);
      }
    }
    return {
      list: result,
      from: replaceToken ? Pos(cur.line, tagStart == null ? token.start : tagStart) : cur,
      to: replaceToken ? Pos(cur.line, token.end) : cur
    };
  }
  
  function getSLD10Hints(cm) {
    var hintOptions = {schemaInfo: sld10tags};
    return getHints(cm, hintOptions);
  }

  CodeMirror.registerHelper("hint", "sld10", getSLD10Hints);
});