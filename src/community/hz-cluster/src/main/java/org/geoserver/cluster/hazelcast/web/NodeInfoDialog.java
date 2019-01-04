/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast.web;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.cluster.hazelcast.HzCluster;
import org.geoserver.web.GeoServerApplication;

public class NodeInfoDialog extends Panel {

    private static final long serialVersionUID = -6118539402031076763L;

    public NodeInfoDialog(String id) {
        super(id);
        HazelcastInstance hz = getHazelcast();

        Member m = hz.getCluster().getLocalMember();
        InetSocketAddress address = m.getSocketAddress();

        add(new Label("groupName", hz.getConfig().getGroupConfig().getName()));
        add(new Label("ip", address.getAddress().getHostAddress()));
        add(new Label("host", address.getHostName()));
        add(new Label("port", String.valueOf(address.getPort())));

        add(
                new WebMarkupContainer("cluster")
                        .add(
                                new ListView<Member>("members", new MembersDetachableModel()) {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    protected void populateItem(ListItem<Member> item) {
                                        Member m = item.getModelObject();
                                        InetSocketAddress address = m.getSocketAddress();
                                        String ip = address.getAddress().getHostAddress();
                                        int port = address.getPort();
                                        String local = m.localMember() ? " (this)" : "";

                                        item.add(
                                                new Label(
                                                        "label",
                                                        String.format("%s:%d%s", ip, port, local)));
                                    }
                                }));
    }

    private static class MembersDetachableModel extends LoadableDetachableModel<List<Member>> {
        private static final long serialVersionUID = 1L;

        @Override
        protected List<Member> load() {
            HazelcastInstance hz = getHazelcast();
            Cluster c = hz.getCluster();
            List<Member> members = new ArrayList<Member>(c.getMembers());
            return members;
        }
    }

    static HazelcastInstance getHazelcast() {
        return GeoServerApplication.get().getBeanOfType(HzCluster.class).getHz();
    }
}
