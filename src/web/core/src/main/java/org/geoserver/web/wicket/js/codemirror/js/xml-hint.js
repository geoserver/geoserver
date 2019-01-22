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
          "sld:StyledLayerDescriptor"
      ],
      "sld:StyledLayerDescriptor": {
          "attrs": {
          "version": null,
          "xsi:schemaLocation": ["http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"],
          "xmlns": ["http://www.opengis.net/sld"],
          "xmlns:gml": ["http://www.opengis.net/gml"],
          "xmlns:ogc": ["http://www.opengis.net/ogc"],
          "xmlns:sld": ["http://www.opengis.net/sld"],
          "xmlns:xlink": ["http://www.w3.org/1999/xlink"],
          "xmlns:xsi": ["http://www.w3.org/2001/XMLSchema-instance"],
          },
          "children": [
          "sld:Name",
          "sld:Title",
          "sld:Abstract",
          "sld:NamedLayer",
          "sld:UserLayer",
          ]
      },
      "sld:Name": {},
      "sld:Title": {
          "children": [
          "sld:Localized"
          ]
      },
      "sld:Abstract": {
          "children": [
          "sld:Localized",
          ]
      },
      "sld:Localized": {
          "attrs": {
          "lang": null
          }
      },
      "sld:NamedLayer": {
          "children": [
          "sld:Name",
          "sld:LayerFeatureConstraints",
          "sld:NamedStyle",
          "sld:UserStyle",
          ]
      },
      "sld:NamedStyle": {
          "children": [
          "sld:Name",
          ]
      },
      "sld:UserLayer": {
          "children": [
          "sld:Name",
          "sld:LayerFeatureConstraints",
          "sld:UserStyle",
          "sld:InlineFeature",
          "sld:RemoteOWS",
          ]
      },
      "sld:InlineFeature": {},
      "sld:RemoteOWS": {
          "children": [
          "sld:Service",
          "sld:OnlineResource",
          ]
      },
      "sld:Service": {},
      "sld:OnlineResource": {
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
      "sld:LayerFeatureConstraints": {
          "children": [
          "sld:FeatureTypeConstraint",
          ]
      },
      "sld:FeatureTypeConstraint": {
          "children": [
          "sld:FeatureTypeName",
          "ogc:Filter",
          "sld:Extent",
          ]
      },
      "sld:FeatureTypeName": {},
      "sld:Extent": {
          "children": [
          "sld:Name",
          "sld:Value",
          ]
      },
      "sld:Value": {},
      "sld:UserStyle": {
          "children": [
          "sld:Name",
          "sld:Title",
          "sld:Abstract",
          "sld:IsDefault",
          "sld:FeatureTypeStyle",
          ]
      },
      "sld:IsDefault": {},
      "sld:FeatureTypeStyle": {
          "children": [
          "sld:Name",
          "sld:Title",
          "sld:Abstract",
          "sld:FeatureTypeName",
          "sld:SemanticTypeIdentifier",
          "sld:Transformation",
          "sld:Rule",
          "sld:VendorOption",
          ]
      },
      "sld:SemanticTypeIdentifier": {},
      "sld:Transformation": {
          "children": [
          "ogc:Function"
          ]
      },
      "sld:Rule": {
          "children": [
          "sld:Name",
          "sld:Title",
          "sld:Abstract",
          "sld:LegendGraphic",
          "sld:MinScaleDenominator",
          "sld:MaxScaleDenominator",
          "sld:LineSymbolizer",
          "sld:PointSymbolizer",
          "sld:PolygonSymbolizer",
          "sld:TextSymbolizer",
          "sld:RasterSymbolizer",
          "ogc:Filter",
          "sld:ElseFilter",
          ]
      },
      "sld:LegendGraphic": {
          "children": [
          "sld:Graphic",
          ]
      },
      "sld:ElseFilter": {},
      "sld:MinScaleDenominator": {},
      "sld:MaxScaleDenominator": {},
      "sld:LineSymbolizer": {
          "attrs": {
          "uom": null
          },
          "children": [
          "sld:Geometry",
          "sld:Stroke",
          "sld:PerpendicularOffset",
          "sld:VendorOption",
          ]
      },
      "sld:Geometry": {
          "children": [
              "ogc:PropertyName",
              "ogc:Function",
              "ogc:Literal",
          ]
      },
      "sld:Stroke": {
          "children": [
          "sld:CssParameter",
          "sld:GraphicFill",
          "sld:GraphicStroke",
          ]
      },
      "sld:CssParameter": {
          "attrs": {
          "name": [
                "stroke-opacity", 
                "stroke-width", 
                "stroke-linejoin", 
                "stroke-linecap", 
                "stroke-dasharray", 
                "stroke-dashoffset",
                "fill",
                "fill-opacity",
                "font-family", 
                "font-style", 
                "font-weight", 
                "font-size"
            ]
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
      "sld:GraphicFill": {
          "children": [
          "sld:Graphic",
          ]
      },
      "sld:GraphicStroke": {
          "children": [
          "sld:Graphic",
          ]
      },
      "sld:PolygonSymbolizer": {
          "attrs": {
          "uom": null
          },
          "children": [
          "sld:Geometry",
          "sld:Fill",
          "sld:Stroke",
          "sld:VendorOption",
          ]
      },
      "sld:Fill": {
          "children": [
          "sld:GraphicFill",
          "sld:CssParameter",
          ]
      },
      "sld:PointSymbolizer": {
          "attrs": {
          "uom": null
          },
          "children": [
          "sld:Geometry",
          "sld:Graphic",
          "sld:VendorOption",
          ]
      },
      "sld:Graphic": {
          "children": [
          "sld:Opacity",
          "sld:Size",
          "sld:Rotation",
          "sld:AnchorPoint",
          "sld:Displacement",
          "sld:ExternalGraphic",
          "sld:Mark",
          ]
      },
      "sld:Opacity": {
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
      "sld:Size": {
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
      "sld:Rotation": {
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
      "sld:ExternalGraphic": {
          "children": [
          "sld:OnlineResource",
          "sld:Format",
          ]
      },
      "sld:Format": {},
      "sld:Mark": {
          "children": [
          "sld:WellKnownName",
          "sld:Fill",
          "sld:Stroke",
          ]
      },
      "sld:WellKnownName": {},
      "sld:TextSymbolizer": {
          "attrs": {
          "uom": null
          },
          "children": [
          "sld:Geometry",
          "sld:Label",
          "sld:Font",
          "sld:LabelPlacement",
          "sld:Halo",
          "sld:Fill",
          "sld:Graphic",
          "sld:Priority",
          "sld:VendorOption",
          ]
      },
      "sld:VendorOption": {
          "attrs": {
          "name": null
          }
      },
      "sld:Priority": {
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
      "sld:Label": {
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
      "sld:Font": {
          "children": [
          "sld:CssParameter",
          ]
      },
      "sld:LabelPlacement": {
          "children": [
          "sld:PointPlacement",
          "sld:LinePlacement",
          ]
      },
      "sld:PointPlacement": {
          "children": [
          "sld:AnchorPoint",
          "sld:Displacement",
          "sld:Rotation",
          ]
      },
      "sld:AnchorPoint": {
          "children": [
          "sld:AnchorPointX",
          "sld:AnchorPointY",
          ]
      },
      "sld:AnchorPointX": {
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
      "sld:AnchorPointY": {
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
      "sld:Displacement": {
          "children": [
          "sld:DisplacementX",
          "sld:DisplacementY",
          ]
      },
      "sld:DisplacementX": {
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
      "sld:DisplacementY": {
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
      "sld:LinePlacement": {
          "children": [
          "sld:PerpendicularOffset",
          ]
      },
      "sld:PerpendicularOffset": {
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
      "sld:Halo": {
          "children": [
          "sld:Radius",
          "sld:Fill",
          ]
      },
      "sld:Radius": {
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
      "sld:RasterSymbolizer": {
          "attrs": {
          "uom": null
          },
          "children": [
          "sld:Geometry",
          "sld:Opacity",
          "sld:ChannelSelection",
          "sld:OverlapBehavior",
          "sld:ColorMap",
          "sld:ContrastEnhancement",
          "sld:ShadedRelief",
          "sld:ImageOutline",
          "sld:VendorOption",
          ]
      },
      "sld:ChannelSelection": {
          "children": [
          "sld:GrayChannel",
          "sld:RedChannel",
          "sld:GreenChannel",
          "sld:BlueChannel",
          ]
      },
      "sld:RedChannel": {
          "children": [
          "sld:SourceChannelName",
          "sld:ContrastEnhancement",
          ]
      },
      "sld:GreenChannel": {
          "children": [
          "sld:SourceChannelName",
          "sld:ContrastEnhancement",
          ]
      },
      "sld:BlueChannel": {
          "children": [
          "sld:SourceChannelName",
          "sld:ContrastEnhancement",
          ]
      },
      "sld:GrayChannel": {
          "children": [
          "sld:SourceChannelName",
          "sld:ContrastEnhancement",
          ]
      },
      "sld:SourceChannelName": {
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
      "sld:OverlapBehavior": {
          "children": [
          "sld:LATEST_ON_TOP",
          "sld:EARLIEST_ON_TOP",
          "sld:AVERAGE",
          "sld:RANDOM",
          ]
      },
      "sld:LATEST_ON_TOP": {},
      "sld:EARLIEST_ON_TOP": {},
      "sld:AVERAGE": {},
      "sld:RANDOM": {},
      "sld:ColorMap": {
          "attrs": {
          "type": null,
          "extended": null
          },
          "children": [
          "sld:ColorMapEntry",
          ]
      },
      "sld:ColorMapEntry": {
          "attrs": {
          "color": null,
          "opacity": null,
          "quantity": null,
          "label": null
          }
      },
      "sld:ContrastEnhancement": {
          "children": [
          "sld:GammaValue",
          "sld:Normalize",
          "sld:Logarithmic",
          "sld:Exponential",
          "sld:Histogram",
          ]
      },
      "sld:Normalize": {
          "children": [
          "sld:VendorOption",
          ]
      },
      "sld:VendorOption": {},
      "sld:Logarithmic": {
          "children": [
          "sld:VendorOption",
          ]
      },
      "sld:Exponential": {
          "children": [
          "sld:VendorOption",
          ]
      },
      "sld:Histogram": {
          "children": [
          "sld:VendorOption",
          ]
      },
      "sld:Normalize": {},
      "sld:Histogram": {},
      "sld:Logarithmic": {},
      "sld:Exponential": {},
      "sld:GammaValue": {
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
      "sld:ShadedRelief": {
          "children": [
          "sld:BrightnessOnly",
          "sld:ReliefFactor",
          ]
      },
      "sld:BrightnessOnly": {},
      "sld:ReliefFactor": {},
      "sld:ImageOutline": {
          "children": [
          "sld:LineSymbolizer",
          "sld:PolygonSymbolizer",
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
          if (tags[childList[i]]["attrs"] == null) {
            result.push("<" + childList[i] + ">");
          } else {
            result.push("<" + childList[i]);
          }
      } else if (tagType != "close") {
        for (var name in tags)
          if (tags.hasOwnProperty(name) && name != "!top" && name != "!attrs" && (!prefix || name.lastIndexOf(prefix, 0) == 0)) {
            if (tags[name]["attrs"] == null) {
              result.push("<" + name + ">");
            } else {
              result.push("<" + name);
            }
          }
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
    if (token.string == "</" && result.length == 1) {
      //Autocomplete closing tag without showing suggestion
      var doc = cm.getDoc();
      doc.replaceRange(result[0], CodeMirror.Pos(cur.line, token.start), CodeMirror.Pos(cur.line, token.end));
      return {
        list: []
      };
    } else {
      return {
        list: result,
        from: replaceToken ? Pos(cur.line, tagStart == null ? token.start : tagStart) : cur,
        to: replaceToken ? Pos(cur.line, token.end) : cur
      };
    }
  }
  
  function isObjectEmpty(obj) {
    for (var prop in obj) {
      if (obj.hasOwnProperty(prop))
        return false;
    }
  
    return true;
  };

  // applies the first prefix replacement found in the string, or returns the string as-is  
  function replaceInString(key, replacements) {
    for (var k in replacements) {
      if (key.startsWith(k + ":")) {
        var local = key.substring(k.length + 1);
        var newPrefix = replacements[k];
        if (newPrefix) {
          return newPrefix + ":" + local;
        } else {
          return local;
        }
      }
    }
    return key;
  };
  
  // applies the replacements to all elements in the array (which is supposed to contain strings)
  function replaceInArray(array, replacements) {
    return array.map(x => replaceInString(x, replacements));
  };
  
  // takes a javascript object defining tags and performs prefix replacements on it
  function replacePrefixes(tags, replacements) {
    var result = {};
    for (var prop in tags) {
      var value = tags[prop];
      if ("attrs" == prop) {
        result[prop] = value;   
      } else if ("0" != prop) {
        if (value.constructor === Array) {
          value = replaceInArray(value, replacements);
        } else if (!isObjectEmpty(value)) {
          value = replacePrefixes(value, replacements);
        }
        var key = replaceInString(prop, replacements);
        result[key] = value;
      }
    }
  
    return result;
  };
  
  function getPrefixReplacements(style, basePrefixes) {
    var styleHeader = style.match(/<.*StyledLayerDescriptor[^]+?>/);
    var replacements = {};

    if (styleHeader != null && styleHeader[0] != null) {
      var styleDeclaration = styleHeader[0];

      var nsMatcher = /xmlns:?([^=]*)="([^"]+)"/g;
      var m;
      while (m = nsMatcher.exec(styleDeclaration)) {
        if (basePrefixes[m[2]] && m[1] != basePrefixes[m[2]]) {
          replacements[basePrefixes[m[2]]] = m[1];
        }
      }
    }
    return replacements;
  }
  
  function getVersion(style) {
    var styleHeader = style.match(/<.*StyledLayerDescriptor[^]+?>/);
    if (styleHeader != null && styleHeader[0] != null) {
        var version = styleHeader[0].match(/version="(.*?)"/);
        if (version != null && version[1] != null) {
            return version[1];
        } else {
            return null;
        }
    }
  }
  
  function getSLD10Hints(cm) {
    var style = cm.getValue();
    var basePrefixes = {
      "http://www.opengis.net/sld": "sld",
      "http://www.opengis.net/ogc": "ogc",
      "http://www.opengis.net/gml": "gml"
    }

    // can only autocomplete version 1.0, disable completion for any other version
    var version = getVersion(style);
    if (version != null && version != "1.0.0") {
      return null;
    }
  
    var replacements = getPrefixReplacements(style, basePrefixes);
    var tags;
    if (isObjectEmpty(replacements)) {
      tags = sld10tags;
    } else {
      tags = replacePrefixes(sld10tags, replacements);
    }
  
    var hintOptions = {
      schemaInfo: tags
    };
    return getHints(cm, hintOptions);
  }

  CodeMirror.registerHelper("hint", "sld10", getSLD10Hints);
});