/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.opengis.ows11.AllowedValuesType;
import net.opengis.ows11.CodeType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.ows11.ValueType;
import net.opengis.wps10.CRSsType;
import net.opengis.wps10.ComplexDataDescriptionType;
import net.opengis.wps10.DataInputsType;
import net.opengis.wps10.DefaultType;
import net.opengis.wps10.DescribeProcessType;
import net.opengis.wps10.InputDescriptionType;
import net.opengis.wps10.LiteralInputType;
import net.opengis.wps10.LiteralOutputType;
import net.opengis.wps10.OutputDescriptionType;
import net.opengis.wps10.ProcessDescriptionType;
import net.opengis.wps10.ProcessDescriptionsType;
import net.opengis.wps10.ProcessOutputsType;
import net.opengis.wps10.SupportedCRSsType;
import net.opengis.wps10.SupportedComplexDataInputType;
import net.opengis.wps10.SupportedComplexDataType;
import net.opengis.wps10.Wps10Factory;

import org.geoserver.ows.Ows11Util;
import org.geoserver.wfs.xml.XSProfile;
import org.geoserver.wps.ppio.BoundingBoxPPIO;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;
import org.springframework.context.ApplicationContext;

/**
 * First-call DescribeProcess class
 *
 * @author Lucas Reed, Refractions Research Inc
 * @author Justin Deoliveira, OpenGEO
 */
public class DescribeProcess {
    WPSInfo wps;
    ApplicationContext context;
    Locale locale;
    XSProfile xsp;
    
    Wps10Factory wpsf = Wps10Factory.eINSTANCE;
    Ows11Factory owsf = Ows11Factory.eINSTANCE; 
    
    /**
     * Maps the primitive types that can still be used in process input/output descriptions to
     * object wrappers that we can use in process descriptions
     */
    static final Map<Class, Class> PRIMITIVE_TO_WRAPPER;

    static {
        PRIMITIVE_TO_WRAPPER = new HashMap<Class, Class>();
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
    }
    
    public DescribeProcess(WPSInfo wps, ApplicationContext context) {
        this.wps = wps;
        this.context = context;
        locale = Locale.getDefault();
        
        //TODO: creating this ever time this operation is performed is sort of silly
        // some sort of singleton would be nice
        xsp = new XSProfile();
    }

    public ProcessDescriptionsType run(DescribeProcessType request) {
        
        ProcessDescriptionsType pds = wpsf.createProcessDescriptionsType();
        pds.setLang("en");
        
        for ( Iterator i = request.getIdentifier().iterator(); i.hasNext(); ) {
            CodeType id = (CodeType) i.next();
            processDescription( id, pds );
        }
        
        return pds;
    }
    
    void processDescription( CodeType id, ProcessDescriptionsType pds ) {
        Name name = Ows11Util.name(id);
        ProcessFactory pf = GeoServerProcessors.createProcessFactory(name);
        if ( pf == null || pf.create(name) == null) {
            throw new WPSException( "No such process: " + id.getValue() );
        }
        
        ProcessDescriptionType pd = wpsf.createProcessDescriptionType();
        pds.getProcessDescription().add( pd );
        
        pd.setProcessVersion( "1.0.0" );
        pd.setIdentifier( Ows11Util.code( id.getValue() ) );
        pd.setTitle( Ows11Util.languageString(pf.getTitle(name)) );
        pd.setAbstract( Ows11Util.languageString(pf.getDescription(name)) );
        pd.setStatusSupported(true);
        pd.setStoreSupported(true);
        
        //data inputs
        DataInputsType inputs = wpsf.createDataInputsType();
        pd.setDataInputs(inputs);
        dataInputs( inputs, pf, name );
        
        //process outputs
        ProcessOutputsType outputs = wpsf.createProcessOutputsType();
        pd.setProcessOutputs( outputs );
        processOutputs( outputs, pf, name );
     }
    
    void dataInputs( DataInputsType inputs, ProcessFactory pf, Name name) {
        for(Parameter<?> p : pf.getParameterInfo(name).values()) {
            InputDescriptionType input = wpsf.createInputDescriptionType();
            inputs.getInput().add( input );
            
            input.setIdentifier( Ows11Util.code( p.key ) );
            input.setTitle( Ows11Util.languageString( p.title ) );
            input.setAbstract( Ows11Util.languageString( p.description ) );
            
            // WPS spec specifies non-negative for unlimited inputs, so -1 -> 0
            input.setMaxOccurs( p.maxOccurs == -1 
                ? BigInteger.valueOf( Long.MAX_VALUE ) : BigInteger.valueOf( p.maxOccurs ) );
             
            input.setMinOccurs( BigInteger.valueOf( p.minOccurs ) );
            
            List<ProcessParameterIO> ppios = ProcessParameterIO.findAll( p, context);
            if ( ppios.isEmpty() ) {
                throw new WPSException( "Could not find process parameter for type " + p.key + "," + p.type );
            }
           
            //handle the literal case
            if (ppios.get( 0 ) instanceof LiteralPPIO ) {
                LiteralPPIO lppio = (LiteralPPIO) ppios.get( 0 );
                
                LiteralInputType literal = wpsf.createLiteralInputType();
                input.setLiteralData( literal );
                
                // map the java class to an xml type name
                if ( !String.class.equals(lppio.getType()) ) {
                	Class type = lppio.getType();
                	if(PRIMITIVE_TO_WRAPPER.containsKey(type)) {
                		type = PRIMITIVE_TO_WRAPPER.get(type);
                	}
                    Name typeName = xsp.name(type); 
                    if ( typeName != null ) {
                        literal.setDataType(Ows11Util.type("xs:" + typeName.getLocalPart()));        
                    }    
                }
                if (p.metadata.get(Parameter.OPTIONS) != null) {
                    List<Object> options = (List<Object>) p.metadata.get(Parameter.OPTIONS);
                    Object[] optionsArray = (Object[]) options.toArray(new Object[options.size()]);
                    addAllowedValues(literal, optionsArray);
                } else if (lppio.getType().isEnum()) {
                	Object[] enumValues = lppio.getType().getEnumConstants();
                    addAllowedValues(literal, enumValues);
                } else {
                	literal.setAnyValue( owsf.createAnyValueType() );
                }

                //TODO: output the default value and see if we can output a valid range as well
            } else if(ppios.get( 0 ) instanceof BoundingBoxPPIO) {
                input.setBoundingBoxData(buildSupportedCRSType());
            } else {
                //handle the complex data case
                SupportedComplexDataInputType complex = wpsf.createSupportedComplexDataInputType();
                input.setComplexData( complex );
                
                complex.setSupported( wpsf.createComplexDataCombinationsType() );
                for ( ProcessParameterIO ppio : ppios ) {
                    ComplexPPIO cppio = (ComplexPPIO) ppio;

                    ComplexDataDescriptionType format = wpsf.createComplexDataDescriptionType();
                    format.setMimeType( cppio.getMimeType() );
                    
                    //add to supported
                    complex.getSupported().getFormat().add( format );
                    
                    //handle the default    
                    if ( complex.getDefault() == null ) {
                        ComplexDataDescriptionType def = wpsf.createComplexDataDescriptionType();
                        def.setMimeType( format.getMimeType() );
                        
                        complex.setDefault( wpsf.createComplexDataCombinationType() );
                        complex.getDefault().setFormat( def );
                    }
                }
            }
        }
    }

    private void addAllowedValues(LiteralInputType literal, Object[] values) {
        AllowedValuesType allowed = owsf.createAllowedValuesType();
        for (Object value : values) {
            ValueType vt = owsf.createValueType();
            vt.setValue(value.toString());
            allowed.getValue().add(vt);
        }
        literal.setAllowedValues(allowed);
    }

    private SupportedCRSsType buildSupportedCRSType() {
        SupportedCRSsType supportedCRS = wpsf.createSupportedCRSsType();
        DefaultType def = wpsf.createDefaultType();
        def.setCRS("EPSG:4326");
        supportedCRS.setDefault(def);
        // TODO: redo the bindings, supported crs should contain a list, not a single value
        CRSsType crss = wpsf.createCRSsType();
        crss.setCRS("EPSG:4326");
        supportedCRS.setSupported(crss);
        return supportedCRS;
    }
    
    void processOutputs( ProcessOutputsType outputs, ProcessFactory pf, Name name) {
        Map<String,Parameter<?>> outs = pf.getResultInfo(name, null);
        for ( Parameter p : outs.values() ) {
            OutputDescriptionType output = wpsf.createOutputDescriptionType();
            outputs.getOutput().add( output );
            
            output.setIdentifier( Ows11Util.code( p.key ) );
            output.setTitle( Ows11Util.languageString( p.title ) );
            
            List<ProcessParameterIO> ppios = ProcessParameterIO.findAll( p, context);
            if ( ppios.isEmpty() ) {
                throw new WPSException( "Could not find process parameter for type " + p.key + "," + p.type );
            }
            
            //handle the literal case
            if ( ppios.get( 0 ) instanceof LiteralPPIO ) {
                LiteralPPIO lppio = (LiteralPPIO) ppios.get( 0 );
                
                LiteralOutputType literal = wpsf.createLiteralOutputType();
                output.setLiteralOutput(literal);
                
                //map the java class to an xml type name
                if ( !String.class.equals( lppio.getType() ) ) {
                	Class type = lppio.getType();
                	if(PRIMITIVE_TO_WRAPPER.containsKey(type)) {
                		type = PRIMITIVE_TO_WRAPPER.get(type);
                	}
                    Name typeName = xsp.name(type); 
                    if ( typeName != null ) {
                        literal.setDataType( Ows11Util.type( typeName.getLocalPart() ) );        
                    }    
                }
            } else if(ppios.get(0) instanceof BoundingBoxPPIO) {
                output.setBoundingBoxOutput(buildSupportedCRSType());
            } else {
                //handle the complex data case
                SupportedComplexDataType complex = wpsf.createSupportedComplexDataType();
                output.setComplexOutput( complex );
                
                complex.setSupported( wpsf.createComplexDataCombinationsType() );
                for ( ProcessParameterIO ppio : ppios ) {
                    ComplexPPIO cppio = (ComplexPPIO) ppio;

                    ComplexDataDescriptionType format = wpsf.createComplexDataDescriptionType();
                    format.setMimeType( cppio.getMimeType() );
                    
                    //add to supported
                    complex.getSupported().getFormat().add( format );
                    
                    //handle the default    
                    if ( complex.getDefault() == null ) {
                        ComplexDataDescriptionType def = wpsf.createComplexDataDescriptionType();
                        def.setMimeType( format.getMimeType() );
                        
                        complex.setDefault( wpsf.createComplexDataCombinationType() );
                        complex.getDefault().setFormat( def );
                    }
                }
            }
        }
    }
}
