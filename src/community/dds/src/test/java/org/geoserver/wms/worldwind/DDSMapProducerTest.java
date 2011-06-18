package org.geoserver.wms.worldwind;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;

import org.apache.commons.io.FileUtils;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMapOutputFormatTest;

public class DDSMapProducerTest extends RenderedImageMapOutputFormatTest {

	private String mapFormat = "image/dds";
	protected RenderedImageMapOutputFormat rasterMapProducer;

	/**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new DDSMapProducerTest());
    }

    protected RenderedImageMapOutputFormat getProducerInstance() {
        return new RenderedImageMapOutputFormat(this.mapFormat, getWMS());
    }
    
    public void setUpInternal() throws Exception {
	    super.setUpInternal();
	    this.rasterMapProducer = this.getProducerInstance();
	}
    
    
	
	public String getMapFormat()
	{
		return this.mapFormat;
	}

	protected void copySchemaFile(String file) throws IOException {
        File f = new File("../../web/app/src/main/webapp/schemas/" + file);
        FileUtils.copyFile(f, getResourceLoader().createFile("WEB-INF/schemas/"+file));
    }
}
