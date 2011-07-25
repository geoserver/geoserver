/*
 *  Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.geoserver.sldservice.rest.resource;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.catalog.rest.SLDFormat;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.MediaTypes;
import org.geoserver.sldservice.utils.classifier.ColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.BlueColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.GrayColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.JetColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.RandomColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.RedColorRamp;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.ColorMap;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.opengis.filter.FilterFactory2;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * @author Alessio Fabiani, GeoSolutions SAS
 */
public class RasterizerResource extends AbstractCatalogResource {

	private final static Logger LOGGER = Logger.getLogger(RasterizerResource.class.toString());
	
	/**
     * media type for SLD
     */
    public static final MediaType MEDIATYPE_SLD = new MediaType( "application/vnd.ogc.sld+xml" );
    static {
        MediaTypes.registerExtension( "sld", MEDIATYPE_SLD );
    }

    public enum COLORRAMP_TYPE {RED, BLUE, GRAY, JET, RANDOM, CUSTOM};
    
    public enum COLORMAP_TYPE {RAMP, INTERVALS, VALUES};

	private static final double DEFAULT_MIN = 0.0;
	private static final double DEFAULT_MAX = 100.0;
	private static final double DEFAULT_MIN_DECREMENT = 0.000000001;
	private static final int DEFAULT_CLASSES = 100;
	private static final int DEFAULT_DIGITS = 5;
	private static final int DEFAULT_TYPE = ColorMap.TYPE_RAMP;

	public RasterizerResource(Context context, Request request, Response response, Catalog catalog) {
		super(context, request, response, StyleInfo.class, catalog);
	}

	@Override
    protected List<DataFormat> createSupportedFormats(Request request,Response response) {
        List<DataFormat> formats = super.createSupportedFormats(request,response);
        formats.add( new SLDFormat() );
        return formats;
    }
	
	@Override
	protected Object handleObjectGet() throws Exception {
		Request req = getRequest();
		Form parameters = req.getResourceRef().getQueryAsForm();
		
		String layer = getAttribute("layer");

		if (layer == null) {
			return new ArrayList();
		}
		
		double min = parameters.getFirstValue("min") != null ? Double.parseDouble(parameters.getFirstValue("min")) : DEFAULT_MIN;
		double max = parameters.getFirstValue("max") != null ? Double.parseDouble(parameters.getFirstValue("max")) : DEFAULT_MAX;
		int classes = parameters.getFirstValue("classes") != null ? Integer.parseInt(parameters.getFirstValue("classes")) : DEFAULT_CLASSES;
		int digits = parameters.getFirstValue("digits") != null ? Integer.parseInt(parameters.getFirstValue("digits")) : DEFAULT_DIGITS;
		final String type = parameters.getFirstValue("type");
		int colormapType = DEFAULT_TYPE;
		if (type != null){
		    if (type.equalsIgnoreCase(COLORMAP_TYPE.INTERVALS.toString())){
		        colormapType = ColorMap.TYPE_INTERVALS;
		    } else if (type.equalsIgnoreCase(COLORMAP_TYPE.VALUES.toString())){
                        colormapType = ColorMap.TYPE_VALUES;
		    }
		}
		
		COLORRAMP_TYPE ramp = parameters.getFirstValue("ramp") != null ? COLORRAMP_TYPE.valueOf(parameters.getFirstValue("ramp").toUpperCase()) : COLORRAMP_TYPE.RED;
		
		if (min == max)
			min = min - Double.MIN_VALUE;
		LayerInfo layerInfo = catalog.getLayerByName(layer);
		if (layerInfo != null) {
			ResourceInfo obj = layerInfo.getResource();
			/* Check if it's feature type or coverage */
			if (obj instanceof CoverageInfo) {
				CoverageInfo cvInfo;
				cvInfo = (CoverageInfo) obj;
				
				StyleInfo defaultStyle = layerInfo.getDefaultStyle();
				RasterSymbolizer rasterSymbolizer = getRasterSymbolizer(defaultStyle);
				
				if (rasterSymbolizer == null) {
					throw new RestletException( "RasterSymbolizer SLD expected!", Status.CLIENT_ERROR_EXPECTATION_FAILED);
				}
				
				Style rasterized = remapStyle(defaultStyle, rasterSymbolizer, min, max, classes, ramp, layer, digits, colormapType); 
				
				//check the format, if specified as sld, return the sld itself
		        DataFormat format = getFormatGet();
				if ( format instanceof SLDFormat ) {
					return rasterized;
		        }
				return defaultStyle;
			}
		}
		
		return new ArrayList();
	}
	
	/**
	 * 
	 * @param defaultStyle
	 * @param rasterSymbolizer
	 * @param layerName 
	 * @return
	 * @throws Exception 
	 */
	private Style remapStyle(StyleInfo defaultStyle, RasterSymbolizer rasterSymbolizer, double min, double max, 
	        int classes, COLORRAMP_TYPE ramp, String layerName, final int digits, final int colorMapType) throws Exception {
		StyleBuilder sb = new StyleBuilder();
		
		ColorMap originalColorMap = rasterSymbolizer.getColorMap();
		ColorMap resampledColorMap = null;
		
		int numClasses = originalColorMap.getColorMapEntries().length;
		
//		if (numClasses > 0) {
//			resampledColorMap = originalColorMap;
//			double res = (max - min) / (numClasses - 1);
//			int c = 0;
//			for (ColorMapEntry cmEntry : resampledColorMap.getColorMapEntries()) {
//				cmEntry.setQuantity(sb.literalExpression(min + res * c));
//				c++;
//			}
//		} else 
		if (classes > 0) {
			final String[] labels = new String[classes+1];
			final double[] quantities = new double[classes+1];
			
			ColorRamp colorRamp = null;
			quantities[0] = min - DEFAULT_MIN_DECREMENT;
			if (colorMapType == ColorMap.TYPE_INTERVALS){
			    max = max + DEFAULT_MIN_DECREMENT;
			    min = min + DEFAULT_MIN_DECREMENT;
			}
			double res = (max - min) / (classes - 1);
			labels[0] = "transparent";
			final String format ="%." + digits + "f";
			for (int c = 1; c <= classes; c++) {
				quantities[c] = min + res * (c-1);
				labels[c] = String.format(Locale.US, format, quantities[c]);
			}
			
			switch (ramp) {
			case RED:
				colorRamp = new RedColorRamp();
				break;
			case BLUE:
				colorRamp = new BlueColorRamp();
				break;
			case GRAY:
				colorRamp = new GrayColorRamp();
				break;
			case JET:
				colorRamp = new JetColorRamp();
				break;
			case RANDOM:
				colorRamp = new RandomColorRamp();
				break;
			}
			colorRamp.setNumClasses(classes);
			
			resampledColorMap = sb.createColorMap(
					labels, 
					quantities, 
					colorRamp.getRamp().toArray(new Color[1]), 
					colorMapType
			);
			FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2(null);
			resampledColorMap.getColorMapEntry(0).setOpacity(filterFactory.literal(0));
		} else {
			return defaultStyle.getStyle();
		}
		
//		StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
//		RasterSymbolizer symbolizer = styleFactory.createRasterSymbolizer();
//		symbolizer.setColorMap(resampledColorMap);
		rasterSymbolizer.setColorMap(resampledColorMap);
		Style style = sb.createStyle(layerName, rasterSymbolizer);
		
		return style;
	}

	/**
	 * 
	 * @param defaultStyle
	 * @return
	 */
	private RasterSymbolizer getRasterSymbolizer(StyleInfo sInfo) {
		RasterSymbolizer rasterSymbolizer = null;
		
		try {
			for (FeatureTypeStyle ftStyle : sInfo.getStyle().getFeatureTypeStyles()) {
				for (Rule rule : ftStyle.getRules()) {
					for (Symbolizer sym : rule.getSymbolizers()) {
						if (sym instanceof RasterSymbolizer) {
							rasterSymbolizer = (RasterSymbolizer) sym;
							break;
						}
					}
					
					if (rasterSymbolizer != null) break;
				}
				
				if (rasterSymbolizer != null) break;
			}
		} catch (IOException e) {
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.log(Level.FINE, "The following exception has occurred " 
						+ e.getLocalizedMessage(), e);
			return null;
		}
		
		return rasterSymbolizer;
	}

	@Override
	public boolean allowPost() {
		return false;
	}

	@Override
	protected String handleObjectPost(Object object) {
		return null;
	}

	@Override
	protected void handleObjectPut(Object object) {
		// do nothing, we do not allow post
	}
}
