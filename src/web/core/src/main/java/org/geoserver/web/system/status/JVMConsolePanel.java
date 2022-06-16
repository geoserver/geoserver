/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.system.status;

import static org.geoserver.web.system.status.ConsoleInfoUtils.getHistoMemoryDump;
import static org.geoserver.web.system.status.ConsoleInfoUtils.getThreadsInfo;

import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;

/** Panel displaying the JVM threads allocated or the heap memory objects */
public class JVMConsolePanel extends Panel {

    private final boolean lockedMonitors = true;
    private final boolean lockedSynchronizers = true;

    private String dumpLog;

    public JVMConsolePanel(String id) {
        super(id);

        add(
                new Link<String>("dumpThread") {
                    private static final long serialVersionUID = 9014754243470867547L;

                    @Override
                    public void onClick() {
                        dumpLog = getThreadsInfo(lockedMonitors, lockedSynchronizers);
                    }
                });

        add(
                new Link<String>("dumpHeap") {
                    private static final long serialVersionUID = 9014754243470867547L;

                    @Override
                    public void onClick() {
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
    }
}
