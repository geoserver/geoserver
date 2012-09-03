/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONException;

import org.apache.commons.io.IOUtils;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.ServiceException;

import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * Enum to hold the MIME type for JSON and some useful related utils
 * <ul>
 *  <li>application/json</li>
 *  <li>text/javascript</li>
 * </ul>
 * 
 * @author Carlo Cancellieri
 */
public enum JSONType {
	JSONP,
	JSON;
	
	/**
	 * The key value into the optional FORMAT_OPTIONS map
	 */
	public final static String CALLBACK_FUNCTION_KEY = "callback";
	/**
	 * The default value of the callback function
	 */
	public final static String CALLBACK_FUNCTION = "paddingOutput";
	
	public final static String json="application/json";
	public final static String jsonp="text/javascript";

	/**
	 * Check if the passed MimeType is a valid jsonp
	 * @param type the MimeType string representation to check
	 * @return true if type is equals to {@link #jsonp}
	 */
	public static boolean isJsonpMimeType(String type) {
    	return  JSONType.jsonp.equals(type);  
    }
    
	/**
	 * Check if the passed MimeType is a valid json
	 * @param type the MimeType string representation to check
	 * @return true if type is equals to {@link #json}
	 */
    public static boolean isJsonMimeType(String type) {
    	return  JSONType.json.equals(type);
    }
	
    /**
     * Return the JSNOType enum matching the passed MimeType or null (if no match)
     * @param mime the mimetype to check
     * @return the JSNOType enum matching the passed MimeType or null (if no match)
     */
	public static JSONType getJSONType(String mime){
		if (json.equalsIgnoreCase(mime)){
			return JSON;
		} else if (jsonp.equalsIgnoreCase(mime)){
			return JSONP;
		} else {
			return null; //not valid representation
		}
	}
	
	/**
	 * get the MimeType for this object
	 * @return return a string representation of the MimeType
	 */
	public String getMimeType(){
		switch (this){
		case JSON:
			return json;
		case JSONP:
			return jsonp;
		default:
			return null;
		}
	}
	
	/**
	 * get an array containing all the MimeType handled by this object
	 * @return return a string array of handled MimeType
	 * @return
	 */
	public static String[] getSupportedTypes(){
		return new String[]{json,jsonp};
	}
	
	/**
	 * Can be used when {@link #jsonp} format is specified to resolve the callback parameter into the FORMAT_OPTIONS map
	 * @param kvp the kay value pair map of the request
	 * @return The string name of the callback function or the default {@link #CALLBACK_FUNCTION} if not found. 
	 */
	public static String getCallbackFunction(Map kvp){
    	if (!(kvp.get("FORMAT_OPTIONS") instanceof Map)) {
        	return JSONType.CALLBACK_FUNCTION;
        } else {
			Map<String, String> map = (Map<String, String>) kvp.get("FORMAT_OPTIONS");
			String callback = map.get(CALLBACK_FUNCTION_KEY);
			if (callback != null) {
				return callback;
			} else {
				return JSONType.CALLBACK_FUNCTION;
			}
        }
    }
	
	
	public static void handleJsonException(Logger LOGGER, ServiceException exception, Request request, String charset, boolean verbose, boolean isJsonp) {
    	
    	final HttpServletResponse response = request.getHttpResponse();
    	response.setContentType(JSONType.jsonp);
        // TODO: server encoding options?
        response.setCharacterEncoding(charset);
        
        ServletOutputStream os = null;
    	try {
    		os=response.getOutputStream();
    		if (isJsonp) {
    			// jsonp
    			JSONType.writeJsonpException(exception,request,os,charset,verbose);
    		} else {
    			// json
    			OutputStreamWriter outWriter = null;
    			try {
    				outWriter = new OutputStreamWriter(os, charset);
    				JSONType.writeJsonException(exception, request, outWriter, verbose);
    			} finally {
    				if (outWriter != null) {
    	    			try {
    	    				outWriter.flush();
    	    			} catch (IOException ioe){}
    					IOUtils.closeQuietly(outWriter);
    				}
    			}

    		}
    	} catch (Exception e){
    		LOGGER.warning(e.getLocalizedMessage());
    	} finally {
    		if (os!=null){
    			try {
    				os.flush();
    			} catch (IOException ioe){}
    			IOUtils.closeQuietly(os);
    		}
    	}
    }
	
    private static void writeJsonpException(ServiceException exception, Request request, OutputStream out, String charset, boolean verbose)
			throws IOException {
		
		OutputStreamWriter outWriter = new OutputStreamWriter(out, charset);
		final String callback;
    	if (request == null) {
			callback=JSONType.CALLBACK_FUNCTION;
		} else {
			callback=JSONType.getCallbackFunction(request.getKvp());
		}
		outWriter.write(callback + "(");

		writeJsonException(exception, request, outWriter, verbose);
	
		outWriter.write(")");
		outWriter.flush();
		IOUtils.closeQuietly(outWriter);
	}
    
    private static void writeJsonException(ServiceException exception, Request request, OutputStreamWriter outWriter, boolean verbose) throws IOException {
		try {
			final JsonWriter jsonWriter = new JsonWriter(outWriter);
			
			jsonWriter.startNode("ExceptionReport", String.class);
				jsonWriter.addAttribute("version", request.getVersion());
				jsonWriter.startNode("Exception", String.class);
					jsonWriter.addAttribute("exceptionCode", exception.getCode()==null?"noApplicableCode":exception.getCode());
					jsonWriter.addAttribute("exceptionLocator", exception.getLocator()==null?"noLocator":exception.getLocator());
					jsonWriter.startNode("ExceptionText", String.class);
					// message
			        if ((exception.getMessage() != null)) {
			            StringBuffer sb=new StringBuffer(exception.getMessage().length());
			            OwsUtils.dumpExceptionMessages(exception, sb, false);

			            if (verbose) {
			            	ByteArrayOutputStream stackTrace=null;
			            	try {
				                stackTrace = new ByteArrayOutputStream();
				                exception.printStackTrace(new PrintStream(stackTrace));        
				                sb.append("\nDetails:\n");
				                sb.append(new String(stackTrace.toByteArray()));
			            	} finally {
			            		IOUtils.closeQuietly(stackTrace);
			            	}
			            }
			            jsonWriter.setValue(sb.toString());
			        }
					jsonWriter.endNode();
				jsonWriter.endNode();
			jsonWriter.endNode();

		} catch (JSONException jsonException) {
			ServiceException serviceException = new ServiceException("Error: "
					+ jsonException.getMessage());
			serviceException.initCause(jsonException);
			throw serviceException;
		}
	}

}