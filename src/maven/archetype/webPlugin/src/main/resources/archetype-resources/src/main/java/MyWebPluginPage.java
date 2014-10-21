/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package ${groupId};

import org.geoserver.web.GeoServerBasePage;
import org.apache.wicket.markup.html.basic.Label;

public class MyWebPluginPage extends GeoServerBasePage {

  public MyWebPluginPage() {
    add(new Label("info"));
  }

}
