/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.system.status;

import java.util.logging.Logger;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geotools.util.logging.Logging;

/** Panel displaying the JVM threads allocated or the heap memory objects */
public class JVMConsolePanel extends Panel implements ConsoleInfoUtils {

    private static final Logger LOGGER = Logging.getLogger(JVMConsolePanel.class);

    private final boolean lockedMonitors = true;
    private final boolean lockedSynchronizers = true;

    private String dumpLog;

    public JVMConsolePanel(String id) {
        super(id);

        add(
                new Link("dumpThread") {
                    private static final long serialVersionUID = 9014754243470867547L;

                    @Override
                    public void onClick() {
                        LOGGER.info("get thread dump");
                        dumpLog = getThreadsInfo(lockedMonitors, lockedSynchronizers);
                    }
                });

        add(
                new Link<String>("dumpHeap") {
                    private static final long serialVersionUID = 9014754243470867547L;

                    @Override
                    public void onClick() {
                        LOGGER.info("get heap dump");
                        dumpLog = getHistoMemoryDump();
                    }
                });

        final TextArea<String> logs =
                new TextArea<>(
                        "dumpContent",
                        new LoadableDetachableModel<String>() {
                            @Override
                            protected String load() {
                                return dumpLog;
                            }
                        });
        logs.setOutputMarkupId(true);
        logs.setMarkupId("dumpContent");
        add(logs);

        add(
                new Link<Object>("download") {

                    @Override
                    public void onClick() {
                        LOGGER.info("download file");
                    }
                });
    }
}
