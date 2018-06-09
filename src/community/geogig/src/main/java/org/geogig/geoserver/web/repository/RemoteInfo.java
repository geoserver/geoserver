/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import static com.google.common.base.Objects.equal;

import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.repository.Remote;

/** A {@link Remote} representation for the presentation layer */
public class RemoteInfo implements Serializable {

    private static final long serialVersionUID = 242699247252608741L;

    private Integer id;

    private String name, URL, userName, password;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RemoteInfo)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        RemoteInfo r = (RemoteInfo) o;
        return equal(name, r.name)
                && equal(URL, r.URL)
                && equal(userName, r.userName)
                && equal(password, r.password);
    }

    @Nullable
    Integer getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(RemoteInfo.class, name, URL, userName, password);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String uRL) {
        URL = uRL;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Remote toRemote() {
        String fetchurl = this.URL;
        String pushurl = this.URL;
        String fetch = "+" + Ref.HEADS_PREFIX + "*:" + Ref.REMOTES_PREFIX + name + "/*";
        boolean mapped = false;
        String mappedBranch = null;
        Remote r =
                new Remote(
                        name, fetchurl, pushurl, fetch, mapped, mappedBranch, userName, password);
        return r;
    }

    public static RemoteInfo create(Remote remote) {
        RemoteInfo ri = new RemoteInfo();

        String name = remote.getName();
        ri.setName(name);
        String url = remote.getFetchURL();
        ri.setURL(url);
        String userName = remote.getUserName();
        ri.setUserName(userName);
        String password = remote.getPassword();
        if (password != null) {
            password = Remote.decryptPassword(password);
        }
        ri.setPassword(password);
        ri.id = ri.hashCode();
        return ri;
    }

    public static ArrayList<RemoteInfo> fromList(List<Remote> remotes) {
        ArrayList<RemoteInfo> ris = new ArrayList<>();
        for (Remote remote : remotes) {
            ris.add(create(remote));
        }
        return ris;
    }
}
