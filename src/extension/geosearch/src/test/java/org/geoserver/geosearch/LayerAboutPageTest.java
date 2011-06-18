package org.geoserver.geosearch;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestletException;
import org.geoserver.test.GeoServerTestSupport;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Status;

import freemarker.template.SimpleHash;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.SimpleSequence;

public class LayerAboutPageTest extends GeoServerTestSupport  {

	public void testGetContext() throws Exception{
		LayerAboutPage lap = new LayerAboutPage();
		lap.setCatalog((Catalog) GeoServerExtensions.bean("catalog"));
		
		Request request = new Request(new Method("GET"), "http://example.org/geoserver/rest/topp/states.html");
		
		SimpleHash context;
		
		//test if it fails the right way with bogus namespace
		boolean hadRestletException = false;
		try{
			context = lap.getContext("badNamespace","badType",request);
		} catch(RestletException e){
			hadRestletException = true;
		}
		assertTrue("getContext did not fail with RestletException when given bogus namespace and type",hadRestletException);
		
		RestletException re = null;
		try{
			context = lap.getContext("sf", "badType", request);
		} catch(RestletException e){
			re = e;
		}
		assertNotNull("getContext should fail when given bogus namespace but good type", re);
        assertEquals("the error code should be a 404", Status.CLIENT_ERROR_NOT_FOUND, re.getStatus());
		

        FeatureTypeInfo fti = getFeatureTypeInfo(MockData.GENERICENTITY);
        fti.getMetadata().put("indexingEnabled", true);
        getCatalog().save(fti);
		context = lap.getContext("sf", "GenericEntity", request);		

		assertEquals("Unexpected value for 'name' in context", ((SimpleScalar) context.get("name")).getAsString(), "sf:GenericEntity");
		assertEquals("Unexpected value for 'title' in context", ((SimpleScalar) context.get("title")).getAsString(), "GenericEntity");		
		assertEquals("Unexpected value for 'abstract' in context", ((SimpleScalar) context.get("abstract")).getAsString(), "abstract about GenericEntity");
		
		//keywords
		assertEquals("Value of 'keywords' in context is not the right size", ((SimpleSequence) context.get("keywords")).size(),1);
		assertEquals("Unexpected value of first template model in 'keywords' of context", ((SimpleScalar) ((SimpleSequence) context.get("keywords")).get(0)).getAsString(), "GenericEntity");
		
		//width and height
		assertEquals("Unexpected value for 'width' in context", ((SimpleNumber) context.get("width")).getAsNumber(), 800);
		assertEquals("Unexpected value for 'height' in context", ((SimpleNumber) context.get("height")).getAsNumber(), 375);
		
		assertEquals("Unexpected value for 'srs' in context", ((SimpleScalar) context.get("srs")).getAsString(), "EPSG:4326");	
		assertEquals("Unexpected value for 'bbox' in context", ((SimpleScalar) context.get("bbox")).getAsString(), "-198.0,-99.0,198.0,99.0");			
		assertEquals("Unexpected value for 'maxResolution' in context", ((SimpleNumber) context.get("maxResolution")).getAsNumber(), 1.546875);
	}
	
	
}
