/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.function.Consumer;

public class GetFeatureCallbackTester implements GetFeatureCallback {

    public static final Consumer<GetFeatureContext> NO_OP = ctx -> {};
    Consumer<GetFeatureContext> contextConsumer = NO_OP;

    public GetFeatureCallbackTester() {}

    @Override
    public void beforeQuerying(GetFeatureContext context) {
        contextConsumer.accept(context);
    }

    public void clear() {
        contextConsumer = NO_OP;
    }
}
