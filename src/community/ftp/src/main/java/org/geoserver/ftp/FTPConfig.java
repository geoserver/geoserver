/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ftp;

import java.net.InetAddress;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Configuration bean for the GeoServer FTP Service.
 * 
 * @author groldan
 * @see FTPConfigLoader
 */
class FTPConfig {

    private static final Integer DEFAULT_FTP_PORT = 8021;

    private static final Integer DEFAULT_IDLE_TIMEOUT_SECONDS = 10;

    private static final String DEFAULT_PASSIVE_PORT_RANGE = "2300-2400";

    public static final String ALL_SERVER_ADDRESSES_FLAG = "";

    public static final String DEFAULT_PASSIVE_ADDRESS = "";

    public static final String DEGAULT_PASSIVE_EXTERNAL_ADDRESS = "";

    private boolean enabled;

    private int ftpPort;

    private int idleTimeout;

    private String serverAddress;

    private String passivePorts;

    private String passiveAddress;

    private String passiveExternalAddress;

    public FTPConfig() {
        setDefaults();
    }

    void setDefaults() {
        enabled = false;
        ftpPort = DEFAULT_FTP_PORT;
        idleTimeout = DEFAULT_IDLE_TIMEOUT_SECONDS;
        serverAddress = ALL_SERVER_ADDRESSES_FLAG;
        passivePorts = DEFAULT_PASSIVE_PORT_RANGE;
        passiveAddress = DEFAULT_PASSIVE_ADDRESS;
        passiveExternalAddress = DEGAULT_PASSIVE_EXTERNAL_ADDRESS;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getFtpPort() {
        return ftpPort;
    }

    public void setFtpPort(Integer ftpPort) {
        this.ftpPort = ftpPort;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Returns the number of seconds during which no network activity is allowed before a session is
     * closed due to inactivity, and defaults to 10 seconds.
     */
    public int getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * Gets the {@link InetAddress} used for binding the local socket. Defaults to null, that is,
     * the server binds to all available network interfaces.
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * Returns the passive ports to be used for data connections. Ports can be defined as single
     * ports, closed or open ranges.
     * <p>
     * Multiple definitions can be separated by commas, for example:
     * <ul>
     * <li>2300 : only use port 2300 as the passive port</li>
     * <li>2300-2399 : use all ports in the range</li>
     * <li>2300- : use all ports larger than 2300</li>
     * <li>2300, 2305, 2400- : use 2300 or 2305 or any port larger than 2400</li>
     * </ul>
     * 
     * Defaults to using any available port > 1023
     * </p>
     */
    public String getPassivePorts() {
        return passivePorts;
    }

    /**
     * The address on which the server will listen to passive data connections, if not set defaults
     * to the same address as the control socket for the session.
     */
    public String getPassiveAddress() {
        return passiveAddress;
    }

    /**
     * Returns the address the server will claim to be listening on in the PASV reply. Useful when
     * the server is behind a NAT firewall and the client sees a different address than the server
     * is using.
     */
    public String getPassiveExternalAddress() {
        return passiveExternalAddress;
    }
    
    public String getDocumentation(){
        return "this is the ftpconfig documentation";
    }
}
