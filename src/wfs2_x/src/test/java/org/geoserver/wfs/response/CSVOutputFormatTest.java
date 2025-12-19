package org.geoserver.wfs.response;

import au.com.bytecode.opencsv.CSVReader;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class CSVOutputFormatTest extends WFSTestSupport {

    private static final String CSV = "text/csv";

    // TODO fix this
    @Test
    public void testCountZero() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "wfs?version=2.0.0&request=GetFeature&typeName=sf:PrimitiveGeoFeature&outputFormat=csv&count=0",
                UTF_8.name());
        assertEquals(CSV, getBaseMimeType(response.getContentType()));
        assertEquals(UTF_8.name(), response.getCharacterEncoding());
        assertEquals("attachment; filename=PrimitiveGeoFeature.csv", response.getHeader("Content-Disposition"));
        List<String[]> lines = readLines(response.getContentAsString(), ',');
        assertEquals(1, lines.size());
    }

    /** Convenience to read the csv content and */
    private List<String[]> readLines(String csvContent, Character separator) throws IOException {
        CSVReader reader = new CSVReader(new StringReader(csvContent), separator);

        List<String[]> result = new ArrayList<>();
        String[] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            result.add(nextLine);
        }
        return result;
    }
}
