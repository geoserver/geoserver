/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.format;

import static org.junit.Assert.*;

import org.geoserver.wfs.WFSTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.moesol.geoserver.sync.filter.Sha1SyncFilterFunction;
import com.moesol.geoserver.sync.json.Sha1SyncJson;
import com.moesol.geoserver.sync.json.Sha1SyncPositionHash;

public class Sha1SyncOutputFormatTest extends WFSTestSupport {
	
	private static final String EXPECTED1_SHA1 = "3899037c4e439b97e9f7f5b8cd593b86b0359340";
	private static final String EXPECTED2_SHA1 = "628244c320602d507c5093117c962ae7a458ad0b";
	
	protected void tearDownInternal() throws Exception {
		setRequestAuth("admin", "password");
		SyncChecksumOutputFormat.JUNIT_SHA1_SYNC.set(null);
    	Sha1SyncFilterFunction.clearThreadLocals();
	}
	
	@Before
	public void beforeTest() {
    	Sha1SyncFilterFunction.clearThreadLocals();
	}
	@After
	public void afterTest() {
		SyncChecksumOutputFormat.JUNIT_SHA1_SYNC.set(null);
    	Sha1SyncFilterFunction.clearThreadLocals();
	}
	
	@Test
	public void testOutputFormat() throws Exception {
        //execute a mock request using the output format
		String result = getAsString("wfs?request=GetFeature&version=2.0.0&typeName=cite:Buildings"
				+ "&outputFormat=SyncChecksum&format_options=ATTRIBUTES:the_geom,FID");

		
        // NOTE: If this test fails check to see if the geoserver sample data changed.
        //

		Sha1SyncJson sync = new Gson().fromJson(result, Sha1SyncJson.class);
		
		assertEquals(0, sync.level());
		assertEquals(1, sync.hashes().size());
		assertEquals("", sync.hashes().get(0).position());
		assertEquals("d3659f943fcdda512093d1c573856e8295201c23", sync.hashes().get(0).summary());
        assertEquals(2L, sync.max());
    }

	@Test
    public void testOutputFormatLevel0() throws Exception {
    	// Version 2.0.1 of geoserver does not support : in the format_options value via \\
    	// "&outputFormat=sha1Sync&format_options=ATTRIBUTES:-all;SYNC:{l\\:0}");
    	SyncChecksumOutputFormat.JUNIT_SHA1_SYNC.set("{l:0, h:[{p:''}]}");
        String result = getAsString("wfs?request=GetFeature&typeName=cite:Buildings" + 
        	"&outputFormat=SyncChecksum&format_options=ATTRIBUTES:-all");
    	Sha1SyncJson sync = new Gson().fromJson(result, Sha1SyncJson.class);
        assertEquals(1, sync.level());
        assertEquals("1b", sync.hashes().get(0).position());
        assertEquals(EXPECTED1_SHA1, sync.hashes().get(0).summary());
        assertEquals("52", sync.hashes().get(1).position());
        assertEquals(EXPECTED2_SHA1, sync.hashes().get(1).summary());
        assertEquals(1L, sync.max());
    }
    @Test
    public void testOutputFormatLevel1() throws Exception {
//    	Logger.getLogger("").setLevel(Level.ALL);
//    	Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
    	// Version 2.0.1 of geoserver does not support : in the format_options value via \\
    	// "&outputFormat=sha1Sync&format_options=ATTRIBUTES:-all;SYNC:{l\\:1}");
    	SyncChecksumOutputFormat.JUNIT_SHA1_SYNC.set("{l:1,h:[{p:'1b'},{p:'52'}]}");
        String result = getAsString("wfs?request=GetFeature&typeName=cite:Buildings" + 
        		"&outputFormat=SyncChecksum&format_options=ATTRIBUTES:-all");
        Sha1SyncJson sync = new Gson().fromJson(result, Sha1SyncJson.class);
        assertEquals(2, sync.level());
        assertEquals(2, sync.hashes().size());
        assertEquals("1bc8", sync.hashes().get(0).position());
        assertEquals(EXPECTED1_SHA1, sync.hashes().get(0).summary());
        assertEquals("5259", sync.hashes().get(1).position());
        assertEquals(EXPECTED2_SHA1, sync.hashes().get(1).summary());
        assertEquals(1L, sync.max());
    }
    @Test
    public void testOutputFormatLevel2() throws Exception {
    	// Version 2.0.1 of geoserver does not support : in the format_options value via \\
    	// "&outputFormat=sha1Sync&format_options=ATTRIBUTES:-all;SYNC:{l\\:2}");
    	SyncChecksumOutputFormat.JUNIT_SHA1_SYNC.set("{l:2,h:[{p:'1bc8'},{p:'5259'}]}");
        String result = getAsString("wfs?request=GetFeature&typeName=cite:Buildings" + 
        "&outputFormat=SyncChecksum&format_options=ATTRIBUTES:-all");
        Sha1SyncJson sync = new Gson().fromJson(result, Sha1SyncJson.class);
        assertEquals(3, sync.level());
        assertEquals(2, sync.hashes().size());
        assertEquals("1bc80f", sync.hashes().get(0).position());
        assertEquals(EXPECTED1_SHA1, sync.hashes().get(0).summary());
        assertEquals("525983", sync.hashes().get(1).position());
        assertEquals(EXPECTED2_SHA1, sync.hashes().get(1).summary());
        assertEquals(1L, sync.max());
    }
    @Test
    public void testOutputFormatLevel2_Partial() throws Exception {
    	// Version 2.0.1 of geoserver does not support : in the format_options value via \\
    	// "&outputFormat=sha1Sync&format_options=ATTRIBUTES:-all;SYNC:{l\\:2}");
    	SyncChecksumOutputFormat.JUNIT_SHA1_SYNC.set("{l:2,h:[{p:'1bc8'}]}");
        String result = getAsString("wfs?request=GetFeature&typeName=cite:Buildings" + 
        	"&outputFormat=SyncChecksum&format_options=ATTRIBUTES:-all");
        Sha1SyncJson sync = new Gson().fromJson(result, Sha1SyncJson.class);
        assertEquals(3, sync.level());
        assertEquals(1, sync.hashes().size());
        assertEquals("1bc80f", sync.hashes().get(0).position());
        assertEquals(EXPECTED1_SHA1, sync.hashes().get(0).summary());
        assertEquals(1L, sync.max());
    }
    @Test
    public void testOutputFormatLevel2_Empty() throws Exception {
    	// Version 2.0.1 of geoserver does not support : in the format_options value via \\
    	// "&outputFormat=sha1Sync&format_options=ATTRIBUTES:-all;SYNC:{l\\:2}");
    	SyncChecksumOutputFormat.JUNIT_SHA1_SYNC.set("{l:2}");
        String result = getAsString("wfs?request=GetFeature&typeName=cite:Buildings" + 
        	"&outputFormat=SyncChecksum&format_options=ATTRIBUTES:-all");
        Sha1SyncJson sync = new Gson().fromJson(result, Sha1SyncJson.class);
        assertEquals(3, sync.level());
        assertEquals(0, sync.hashes().size());
        assertEquals(1L, sync.max());
    }
    @Test
    public void testOutputFormat2() throws Exception {
    	// Version 2.0.1 of geoserver does not support : in the format_options value via \\
    	// "&outputFormat=sha1Sync&format_options=ATTRIBUTES:-all;SYNC:{l\\:-1}");
    	SyncChecksumOutputFormat.JUNIT_SHA1_SYNC.set("{l: -1}");
        String result = getAsString("wfs?request=GetFeature&typeName=cite:Buildings" + 
        "&outputFormat=SyncChecksum&format_options=ATTRIBUTES:-all");
        System.out.println("result: " + result);
        Sha1SyncJson sync = new Gson().fromJson(result, Sha1SyncJson.class);
        assertEquals(0, sync.level());
        assertEquals(1, sync.hashes().size());
        assertEquals("", sync.hashes().get(0).position());
        assertEquals("103d2f424918116a0e95dffd91f9ca36e1657ad0", sync.hashes().get(0).summary());
        assertEquals(2L, sync.max());
    }
    @Test
    public void testOutputFormat3() throws Exception {
        String result = getAsString("wfs?request=GetFeature&typeName=cite:Buildings" + 
        		"&outputFormat=SyncChecksum");
        Sha1SyncJson sync = new Gson().fromJson(result, Sha1SyncJson.class);
        assertEquals(0, sync.level());
        assertEquals(1, sync.hashes().size());
        assertEquals("", sync.hashes().get(0).position());
        assertEquals("883aa2473212d6cd9c40db6df655b803b6b69de3", sync.hashes().get(0).summary());
        assertEquals(2L, sync.max());
    }
    @Test
    public void testPostSync() throws Exception {
//    	Logger.getLogger("").setLevel(Level.FINER);
//    	Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
    	
        setRequestAuth("admin", "password");
        String tmpl = "<wfs:GetFeature " 
	        + "service=\"WFS\" " 
	        + "outputFormat=\"SyncChecksum\" "
	        + "version=\"1.1.0\" "
	        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
	        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
	        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
	        +  "<wfs:Query typeName=\"cite:Buildings\"> "
	        +   "<ogc:Filter>"
	        +    "<ogc:PropertyIsEqualTo> "
	        +      "<ogc:Function name=\"sha1Sync\"> "
	        +       "<ogc:Literal>-all</ogc:Literal> "
	        +       "<ogc:Literal>%s</ogc:Literal> "
	        +      "</ogc:Function> "
	        +      "<ogc:Literal>true</ogc:Literal> "
	        +    "</ogc:PropertyIsEqualTo> "
	        +   "</ogc:Filter> "
	        +  "</wfs:Query> " 
	        + "</wfs:GetFeature>";
        Sha1SyncJson client = new Sha1SyncJson().level(0).hashes(
        		new Sha1SyncPositionHash().position("").summary("abc123")
        );
        String clientJson = new Gson().toJson(client);
        String xml = String.format(tmpl, clientJson);
//        System.out.println(xml);
        
        String result = postAsServletResponse("wfs", xml).getOutputStreamContent();
        Sha1SyncJson sync = new Gson().fromJson(result, Sha1SyncJson.class);
        assertEquals(1, sync.level());
        assertEquals(2, sync.hashes().size());
        assertEquals("1b", sync.hashes().get(0).position());
        assertEquals(EXPECTED1_SHA1, sync.hashes().get(0).summary());
        assertEquals(1L, sync.max());
    }
    @Test
    public void testPostSyncWithBadFunctionCall() throws Exception {
        String xml = "<wfs:GetFeature " 
	        + "service=\"WFS\" " 
	        + "outputFormat=\"SyncChecksum\" "
	        + "version=\"1.1.0\" "
	        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
	        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
	        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
	        +  "<wfs:Query typeName=\"cite:Buildings\"> "
	        +   "<ogc:Filter>"
	        +    "<ogc:PropertyIsEqualTo> "
	        +      "<ogc:Function name=\"sha1Sync\"> "
	        +      "</ogc:Function> "
	        +      "<ogc:Literal>true</ogc:Literal> "
	        +    "</ogc:PropertyIsEqualTo> "
	        +   "</ogc:Filter> "
	        +  "</wfs:Query> " 
	        + "</wfs:GetFeature>";

        String out = postAsServletResponse( "wfs", xml ).getOutputStreamContent();
        assertTrue(out, out.contains("Unable to find function sha1Sync"));
    }

    /**
     * Don't use sha1Sync output format and see if we get filtered features.
     * @throws Exception
     */
    @Test
    public void testPostSyncFilter() throws Exception {
        setRequestAuth("admin", "password");
    	String tmpl = "<wfs:GetFeature " 
	        + "service=\"WFS\" " 
	        + "version=\"1.1.0\" "
	        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
	        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
	        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
	        +  "<wfs:Query typeName=\"cite:Buildings\"> "
	        +   "<ogc:Filter>"
	        +    "<ogc:PropertyIsEqualTo> "
	        +      "<ogc:Function name=\"sha1Sync\"> "
	        +       "<ogc:Literal>-all</ogc:Literal> "
	        +       "<ogc:Literal>%s</ogc:Literal> "
	        +      "</ogc:Function> "
	        +      "<ogc:Literal>true</ogc:Literal> "
	        +    "</ogc:PropertyIsEqualTo> "
	        +   "</ogc:Filter> "
	        +  "</wfs:Query> " 
	        + "</wfs:GetFeature>";
        Sha1SyncJson client = new Sha1SyncJson().level(1).hashes(
				new Sha1SyncPositionHash().position("64").summary("70e1fd1423bedd80c917c55e858b88261396a1d9"),
				new Sha1SyncPositionHash().position("ff").summary("2e0d1f5425c8eb74040c899f64fb331ca6ea6559")
        );
        String clientJson = new Gson().toJson(client);
        String xml = String.format(tmpl, clientJson);

        String result = postAsServletResponse( "wfs", xml ).getOutputStreamContent();
        System.out.println(result);
        System.out.format("%s/%s/%s%n", 
        		Sha1SyncFilterFunction.getFormatOptions(), 
        		Sha1SyncFilterFunction.getSha1SyncJson(),
        		Sha1SyncFilterFunction.getFeatureSha1s());
        
        System.out.println("********* Second run");
        result = postAsServletResponse( "wfs", xml ).getOutputStreamContent();
        System.out.println(result);
    }
    @Test
    public void testDescribeFeatureTypeList() throws Exception {
    	setRequestAuth("admin", "password");
    	String result = getAsString("wfs?request=DescribeFeatureType"); 
        System.out.println("result: " + result);
    }
	@Test
	public void testDescribeFeatureBuildings() throws Exception {
		setRequestAuth("admin", "password");
		String result = getAsString("wfs?request=DescribeFeatureType&typeName=cite:Buildings"); 
		System.out.println("result: " + result);
	}
	@Test
	public void testGetFeatureType() throws Exception {
		setRequestAuth("admin", "password");
		String result = getAsString("wfs?request=GetFeature&typeName=cite:Buildings"); 
		System.out.println("result: " + result);
	}

}
