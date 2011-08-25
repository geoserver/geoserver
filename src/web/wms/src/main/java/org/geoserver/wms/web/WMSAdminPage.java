/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.FileExistsValidator;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WatermarkInfo.Position;
import org.geotools.referencing.CRS;

/**
 * Edits the WMS service details 
 */
@SuppressWarnings("serial")
public class WMSAdminPage extends BaseServiceAdminPage<WMSInfo> {
    
    static final List<String> SVG_RENDERERS = Arrays.asList(new String[] {WMS.SVG_BATIK, WMS.SVG_SIMPLE});
    
    static final List<String> KML_REFLECTOR_MODES = Arrays.asList(new String[] {WMS.KML_REFLECTOR_MODE_REFRESH, 
            WMS.KML_REFLECTOR_MODE_SUPEROVERLAY, WMS.KML_REFLECTOR_MODE_DOWNLOAD});
    
    static final List<String> KML_SUPEROVERLAY_MODES = Arrays.asList(new String[] {WMS.KML_SUPEROVERLAY_MODE_AUTO, 
            WMS.KML_SUPEROVERLAY_MODE_RASTER, WMS.KML_SUPEROVERLAY_MODE_OVERVIEW, WMS.KML_SUPEROVERLAY_MODE_HYBRID, WMS.KML_SUPEROVERLAY_MODE_CACHED});
    
    protected Class<WMSInfo> getServiceClass() {
        return WMSInfo.class;
    }
    
    protected void build(IModel info, Form form) {
        // limited srs list
        TextArea srsList = new TextArea("srs", LiveCollectionModel.list(new PropertyModel(info, "sRS"))) {
            @Override
            public IConverter getConverter(Class type) {
                return new SRSListConverter();
            }
                
        };
        srsList.add(new SRSListValidator());
        srsList.setType(List.class);
        form.add(srsList);

        // general
        form.add(new DropDownChoice("interpolation", Arrays.asList(WMSInfo.WMSInterpolation.values()), new InterpolationRenderer()));
        // resource limits
        TextField<Integer> maxMemory = new TextField<Integer>("maxRequestMemory");
        maxMemory.add(new MinimumValidator<Integer>(0));
        form.add(maxMemory);
        TextField<Integer> maxTime = new TextField<Integer>("maxRenderingTime");
        maxTime.add(new MinimumValidator<Integer>(0));
        form.add(maxTime);
        TextField<Integer> maxErrors = new TextField<Integer>("maxRenderingErrors");
        maxErrors.add(new MinimumValidator<Integer>(0));
        form.add(maxErrors);
    	// watermark
    	form.add(new CheckBox("watermark.enabled"));
    	form.add(new TextField("watermark.uRL").add(new FileExistsValidator(true)));
    	TextField<Integer> transparency = new TextField<Integer>("watermark.transparency");
    	transparency.add(new RangeValidator<Integer>(0,100));
        form.add(transparency);
    	form.add(new DropDownChoice("watermark.position", Arrays.asList(Position.values()), new WatermarkPositionRenderer()));
    	// svg
    	PropertyModel metadataModel = new PropertyModel(info, "metadata");
        form.add(new CheckBox("svg.antialias", new MapModel(metadataModel, "svgAntiAlias")));
    	form.add(new DropDownChoice("svg.producer", new MapModel(metadataModel, "svgRenderer"), SVG_RENDERERS, new SVGMethodRenderer()));
    	// png compression levels
    	MapModel pngCompression = defaultedModel(metadataModel, WMS.PNG_COMPRESSION, WMS.PNG_COMPRESSION_DEFAULT);
        TextField<Integer> pngCompressionField = new TextField<Integer>("png.compression", pngCompression, Integer.class);
        pngCompressionField.add(new RangeValidator<Integer>(0, 100));
        form.add(pngCompressionField);
        // jpeg compression levels
    	MapModel jpegCompression = defaultedModel(metadataModel, WMS.JPEG_COMPRESSION, WMS.JPEG_COMPRESSION_DEFAULT);
        TextField<Integer> jpegCompressionField = new TextField<Integer>("jpeg.compression", jpegCompression, Integer.class);
        jpegCompressionField.add(new RangeValidator<Integer>(0,100));
        form.add(jpegCompressionField);
        // GIF animated
        // MAX_ALLOWED_FRAMES
        MapModel maxAllowedFrames = defaultedModel(metadataModel, WMS.MAX_ALLOWED_FRAMES, WMS.MAX_ALLOWED_FRAMES_DEFAULT);
        TextField<Integer> maxAllowedFramesField = new TextField<Integer>("anim.maxallowedframes", maxAllowedFrames, Integer.class);
        maxAllowedFramesField.add(new RangeValidator<Integer>(0, Integer.MAX_VALUE));
        form.add(maxAllowedFramesField);
        // MAX_RENDERING_TIME
        MapModel maxRenderingTime = defaultedModel(metadataModel, WMS.MAX_RENDERING_TIME, null);
        TextField<Integer> maxRenderingTimeField = new TextField<Integer>("anim.maxrenderingtime", maxRenderingTime, Integer.class);
        form.add(maxRenderingTimeField);
        // MAX_RENDERING_SIZE
        MapModel maxRenderingSize = defaultedModel(metadataModel, WMS.MAX_RENDERING_SIZE, null);
        TextField<Integer> maxRenderingSizeField = new TextField<Integer>("anim.maxrenderingsize", maxRenderingSize, Integer.class);
        form.add(maxRenderingSizeField);
        // FRAMES_DELAY
        MapModel framesDelay = defaultedModel(metadataModel, WMS.FRAMES_DELAY, WMS.FRAMES_DELAY_DEFAULT);
        TextField<Integer> framesDelayField = new TextField<Integer>("anim.framesdelay", framesDelay, Integer.class);
        framesDelayField.add(new RangeValidator<Integer>(0, Integer.MAX_VALUE));
        form.add(framesDelayField);
        // LOOP_CONTINUOUSLY
        MapModel loopContinuously = defaultedModel(metadataModel, WMS.LOOP_CONTINUOUSLY, WMS.LOOP_CONTINUOUSLY_DEFAULT);
        CheckBox loopContinuouslyField = new CheckBox("anim.loopcontinuously", loopContinuously);
        form.add(loopContinuouslyField);
        
        // kml handling
        MapModel kmlReflectorMode = defaultedModel(metadataModel, WMS.KML_REFLECTOR_MODE, WMS.KML_REFLECTOR_MODE_DEFAULT);
        form.add(new DropDownChoice("kml.defaultReflectorMode", kmlReflectorMode, KML_REFLECTOR_MODES));
        
        MapModel kmlSuperoverlayMode = defaultedModel(metadataModel, WMS.KML_SUPEROVERLAY_MODE, WMS.KML_SUPEROVERLAY_MODE_DEFAULT);
        form.add(new DropDownChoice("kml.superoverlayMode", kmlSuperoverlayMode, KML_SUPEROVERLAY_MODES));
        
        form.add(new CheckBox("kml.kmattr", defaultedModel(metadataModel, WMS.KML_KMLATTR, WMS.KML_KMLATTR_DEFAULT)));
        form.add(new CheckBox("kml.kmlplacemark", defaultedModel(metadataModel, WMS.KML_KMLPLACEMARK, WMS.KML_KMLPLACEMARK_DEFAULT)));
        
        MapModel kmScore = defaultedModel(metadataModel, WMS.KML_KMSCORE, WMS.KML_KMSCORE_DEFAULT);
        TextField<Integer> kmScoreField = new TextField<Integer>("kml.kmscore", kmScore, Integer.class);
        kmScoreField.add(new RangeValidator<Integer>(0, 100));
        form.add(kmScoreField);
    }
    
    MapModel defaultedModel(IModel baseModel, String key, Object defaultValue) {
        MapModel model = new MapModel(baseModel, key);
        if(model.getObject() == null)
            model.setObject(defaultValue);
        return model;
    }
    
    protected String getServiceName(){
        return "WMS";
    }

    private class WatermarkPositionRenderer implements  IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel(((Position) object).name(), WMSAdminPage.this, null).getString();
        }

        public String getIdValue(Object object, int index) {
            return ((Position) object).name();
        }
        
    }
    
    private class InterpolationRenderer implements  IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel(((WMSInterpolation) object).name(), WMSAdminPage.this, null).getString();
        }

        public String getIdValue(Object object, int index) {
            return ((WMSInterpolation) object).name();
        }
        
    }
    
    private class SVGMethodRenderer implements  IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel("svg." + object, WMSAdminPage.this, null).getString();
        }

        public String getIdValue(Object object, int index) {
            return (String) object;
        }
        
    }
    
    private static class SRSListConverter implements IConverter {
            static final Pattern COMMA_SEPARATED = Pattern.compile("\\s*,\\s*", Pattern.MULTILINE); 
            
            public String convertToString(Object value, Locale locale) {
                List<String> srsList = (List<String>) value;
                if(srsList.isEmpty())
                    return "";
                    
                StringBuffer sb = new StringBuffer();
                for (String srs : srsList) {
                    sb.append(srs).append(", ");
                }
                sb.setLength(sb.length() - 2);
                return sb.toString();
            }
            
            public Object convertToObject(String value, Locale locale) {
                if(value == null || value.trim().equals(""))
                    return Collections.emptyList();
                return new ArrayList<String>(Arrays.asList(COMMA_SEPARATED.split(value)));
            }
    }
    
    private static class SRSListValidator extends AbstractValidator {

        @Override
        protected void onValidate(IValidatable validatable) {
            List<String> srsList = (List<String>) validatable.getValue();
            List<String> invalid = new ArrayList<String>();
            for (String srs : srsList) {
                try {
                    CRS.decode("EPSG:" + srs);
                } catch(Exception e) {
                    invalid.add(srs);
                }
            }
            
            if(invalid.size() > 0)
                error(validatable, "WMSAdminPage.unknownEPSGCodes", Collections.singletonMap("codes", invalid.toString()));
            
        }
        
    }
}
