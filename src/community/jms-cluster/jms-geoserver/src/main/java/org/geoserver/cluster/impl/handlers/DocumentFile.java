/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;

/**
 * 
 * Class used to handle a JDOM parse-able xml file
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class DocumentFile {

	private final File path;
	private final String body;
	/**
	 * @return the path
	 */
	public final File getPath() {
		return path;
	}
	
	/**
	 * @return the body containing the parsed file
	 */
	public final String getBody() {
		return body;
	}
	
	/**
	 * Constructor
	 * @param path the path referring to this file
	 * @param document the string containing the body of the file (should be a valid JDOM document)
	 * @throws JDOMException
	 * @throws IOException
	 */
	public DocumentFile(File path, final String document) throws JDOMException, IOException {
		if (!path.exists()){
			throw new IllegalArgumentException("Unable to locate the file path: \'"+path+"\'");
		}
		this.path = path;
		this.body = document;
	}
	
	public DocumentFile(File path) throws JDOMException, IOException {
		if (!path.exists()){
			throw new IllegalArgumentException("Unable to locate the file path: \'"+path+"\'");
		}
		this.path = path;
		this.body = reader(path);
	}
	
	/**
	 * write the body to the passed file argument
	 * @param file
	 * @throws JDOMException
	 * @throws IOException
	 */
	public void writeTo(File file) throws JDOMException, IOException{
		writer(file, body);
	}

	/**
	 * 
	 * @param xmlString
	 * @return
	 * @throws JDOMException
	 * @throws IOException
	 */
	protected static Document parser(String xmlString) throws JDOMException, IOException {
		InputSource source=null;
		StringReader reader=null;
		try {
			reader=new StringReader(xmlString);
			source = new InputSource(reader);
			final SAXBuilder builder = new SAXBuilder();
			final Document doc = builder.build(source);
			return doc;
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}
	
	private static String reader(File file) throws JDOMException, IOException {
		FileReader reader=null;
		try {
			reader=new FileReader(file);
			final SAXBuilder builder = new SAXBuilder();
			XMLOutputter outputter=new XMLOutputter();
			final Document doc = builder.build(reader);
			return outputter.outputString(doc);
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	/**
	 * 
	 * @param file
	 * @param xml
	 * @throws JDOMException
	 * @throws IOException
	 */
	protected static void writer(File file, String xml) throws JDOMException, IOException {
		
			FileWriter writer=null;
			StringReader reader=null;
			try {
				writer=new FileWriter(file);
				reader=new StringReader(xml);
				
				char[] cbuf=new char[2048];
				int size=0;
				while (reader.ready() && (size=reader.read(cbuf))!=-1){
					writer.write(cbuf,0,size);
				}
				
		} finally {
			writer.flush();
			IOUtils.closeQuietly(writer);
			IOUtils.closeQuietly(reader);
		}
	}

	
}
