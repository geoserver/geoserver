package org.geoserver.data.test;

import java.io.File;
import java.io.IOException;

import org.geoserver.data.util.IOUtils;

public class LiveSystemTestData extends SystemTestData {

    protected File source;

    public LiveSystemTestData(File source) throws IOException {
        super();
        this.source = source;
    }

    @Override
    public void setUp() throws Exception {
        data = IOUtils.createRandomDirectory("./target", "live", "data");
        IOUtils.deepCopy(source, data);
    }

    public void tearDown() throws Exception {
        if(data != null) {
            IOUtils.delete(data);
        }
    }

}
