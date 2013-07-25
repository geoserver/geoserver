package org.geoserver.cluster.hazelcast.web;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;

import static org.geoserver.cluster.hazelcast.HazelcastUtil.*;
import static com.google.common.base.Predicates.*;

public class NodeInfoDialog extends Panel {

    public NodeInfoDialog(String id) {
        super(id);
        HazelcastInstance hz = getHazelcast();

        Member m = hz.getCluster().getLocalMember();
        InetSocketAddress address = m.getInetSocketAddress();

        add(new Label("groupName", hz.getConfig().getGroupConfig().getName()));
        add(new Label("ip", address.getAddress().getHostAddress()));
        add(new Label("host", address.getHostName()));
        add(new Label("port", new Model(address.getPort())));

        Cluster c = hz.getCluster();
        List<Member> members = 
            new ArrayList(Collections2.filter(c.getMembers(), not(equalTo(c.getLocalMember()))));
        
        add(new WebMarkupContainer("cluster")
            .add(new ListView<Member>("members", members) {
                @Override
                protected void populateItem(ListItem<Member> item) {
                    Member m = item.getModelObject();
                    InetSocketAddress address = m.getInetSocketAddress();
                    String ip = address.getAddress().getHostAddress();
                    int port = address.getPort();

                    item.add(new Label("label", 
                        String.format("%s:%d", ip, port)));
                }
            }));
    }

    HazelcastInstance getHazelcast() {
        return GeoServerApplication.get().getBeanOfType(HazelcastInstance.class);
    }
}
