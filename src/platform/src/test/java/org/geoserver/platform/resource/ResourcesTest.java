package org.geoserver.platform.resource;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ResourcesTest {
    class PropertiesRead implements Content.Read<Properties> {

        @Override
        public Properties read(InputStream in) throws Exception {
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        }
        
    }
    private File sample;
    @Before
    public void setUp() throws Exception {
        Properties data = new Properties();
        data.put("key", "value");
        data.put("timestamp", String.valueOf(System.currentTimeMillis()) );
        
        sample = File.createTempFile("sample", "properties");
        write(sample,data);
    }

    private void write(File file, Properties data) throws FileNotFoundException, IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            data.store( out, "Sample properties" );
        }
        finally{
            out.close();
        }
    }
    
    @After
    public void tearDown(){
        if( sample.exists() ){
            sample.delete();
        }
    }
    @Test
    public void testRead() throws Exception {
        Content<Properties> data = Resources.watch(sample, new PropertiesRead() );
        assertEquals( 2, data.content().size() );
        
        write( sample, new Properties() );
        assertEquals( 0, data.content().size() );        
    }
    
    @Test
    public void testCache() throws Exception {
        Content<Properties> data = Resources.watch(sample, new PropertiesRead() );
        data = Resources.cache(data, 1, TimeUnit.SECONDS );
        assertEquals( 2, data.content().size() );        
        write( sample, new Properties() );        
        assertEquals( 2, data.content().size() );
        
        Thread.sleep( 2000 ); // two seconds
        assertEquals( 0, data.content().size() );        
    }

}
