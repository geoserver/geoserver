/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 - 2016 Boundless Spatial Inc.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ysld;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.io.File;
import java.io.PrintWriter;
import org.geotools.api.style.ExternalGraphic;
import org.geotools.api.style.PointSymbolizer;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.styling.SLD;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ResourceLocatorTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testRelativePathWithDefaultResourceLocator() throws Exception {
        YsldHandler handler = new YsldHandler();

        // Want a real file to hold the YSLD
        File file = testFolder.newFile();
        try (PrintWriter out = new PrintWriter(file); ) {
            out.print(
                    """
                    feature-styles:
                    - name: name
                      rules:
                      - symbolizers:
                        - point:
                            size: 32
                            symbols:
                            - external:
                                url: smileyface.png
                                format: image/png
                    """);
        }

        // A file in the same directory
        File image = testFolder.newFile("smileyface.png");

        // ResourceLocator is null so it makes a default.
        StyledLayerDescriptor sld = handler.parse(file, null, null, null);

        PointSymbolizer p = SLD.pointSymbolizer(SLD.defaultStyle(sld));

        assertThat(p.getGraphic().graphicalSymbols().get(0), instanceOf(ExternalGraphic.class));
        ExternalGraphic eg = (ExternalGraphic) p.getGraphic().graphicalSymbols().get(0);

        // It should point to the image
        assertThat(eg.getLocation(), equalTo(image.toURI().toURL()));
        assertThat(eg.getOnlineResource().getLinkage(), equalTo(image.toURI()));
    }
}
