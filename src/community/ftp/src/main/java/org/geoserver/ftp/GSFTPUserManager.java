/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ftp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geotools.util.logging.Logging;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Maps GeoServer users to Apache's FTP Server {@link User}s.
 * <p>
 * <h2>User home directory</h2>
 * If the logged in user has administrative privileges, the home directory is set to the geoserver
 * data root directory (e.g {@code <gs data dir>/data}). For non admin users, the home directory is
 * set to a subdirectory of the geoserver data root directory called the same than the user name
 * (e.g. {@code <gs data dir>/data/incoming/<user name>}).
 * </p>
 * 
 * @author aaime
 * @author groldan
 */
public class GSFTPUserManager implements org.apache.ftpserver.ftplet.UserManager {

    private static final Logger LOGGER = Logging.getLogger(GSFTPUserManager.class);

    /**
     * The role given to the administrators
     */
    private static final String ADMIN_ROLE = GeoServerRole.ADMIN_ROLE.getAuthority();

    /**
     * The default user
     */
    private static final String DEFAULT_USER = GeoServerUser.ADMIN_USERNAME;

    /**
     * The default password
     */
    private static final String DEFAULT_PASSWORD = GeoServerUser.DEFAULT_ADMIN_PASSWD;

    private GeoServerDataDirectory dataDir;

    private AuthenticationManager authManager;

    public GSFTPUserManager(GeoServerDataDirectory dataDir) {
        this.dataDir = dataDir;
    }

    public void setAuthenticationManager(AuthenticationManager authManager) {
        this.authManager = authManager;
    }

    /**
     * @param ftpAuthRequest
     *            one of {@link org.apache.ftpserver.usermanager.AnonymousAuthentication} or
     *            {@link org.apache.ftpserver.usermanager.UsernamePasswordAuthentication}
     * @throws AuthenticationFailedException
     *             if given an {@code AnonymousAuthentication}, or an invalid/disabled user
     *             credentials
     * @see UserManager#authenticate(Authentication)
     */
    public User authenticate(final Authentication ftpAuthRequest)
            throws AuthenticationFailedException {
        if (!(ftpAuthRequest instanceof UsernamePasswordAuthentication)) {
            throw new AuthenticationFailedException();
        }
        final UsernamePasswordAuthentication upa = (UsernamePasswordAuthentication) ftpAuthRequest;
        final String principal = upa.getUsername();
        final String credentials = upa.getPassword();
        org.springframework.security.core.Authentication gsAuth = new UsernamePasswordAuthenticationToken(
                principal, credentials);
        try {
            gsAuth = authManager.authenticate(gsAuth);
        } catch (org.springframework.security.core.AuthenticationException authEx) {
            throw new AuthenticationFailedException(authEx);
        }

        try {
            // gather the user
            BaseUser user = getUserByName(principal);
            user.setPassword(credentials);
            // is the user enabled?
            if (!user.getEnabled()) {
                throw new AuthenticationFailedException();
            }

            // scary message for admins if the username/password has not
            // been changed
            if (DEFAULT_USER.equals(user.getName()) && DEFAULT_PASSWORD.equals(credentials)) {
                LOGGER.log(Level.SEVERE, "The default admin/password combination has not been "
                        + "modified, this makes the embedded FTP server an "
                        + "open file host for everybody to use!!!");
            }

            final File dataRoot = dataDir.findOrCreateDataRoot();

            // enable only admins and non anonymous users
            boolean isGSAdmin = false;
            for (GrantedAuthority authority : gsAuth.getAuthorities()) {
                final String userRole = authority.getAuthority();
                if (ADMIN_ROLE.equals(userRole)) {
                    isGSAdmin = true;
                    break;
                }
            }

            final File homeDirectory;
            if (isGSAdmin) {
                homeDirectory = dataRoot;
            } else {
                /*
                 * This resolves the user's home directory to data/incoming/<user name> but does not
                 * create the directory if it does not already exist. That is left to when the user
                 * is authenticated, check the authenticate() method above.
                 */
                homeDirectory = new File(new File(dataRoot, "incoming"), user.getName());
            }
            String normalizedPath = homeDirectory.getAbsolutePath();
            normalizedPath = FilenameUtils.normalize(normalizedPath);
            user.setHomeDirectory(normalizedPath);
            if (!homeDirectory.exists()) {
                LOGGER.fine("Creating FTP home directory for user " + user.getName() + " at "
                        + normalizedPath);
                homeDirectory.mkdirs();
            }

            return user;
        } catch (AuthenticationFailedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "FTP authentication failure", e);
            throw new AuthenticationFailedException(e);
        }
    }

    /**
     * @throws FtpException
     *             always, operation not supported.
     * @see org.apache.ftpserver.ftplet.UserManager#delete(java.lang.String)
     */
    public void delete(String username) throws FtpException {
        throw new FtpException("No custom user handling on this instance");
    }

    /**
     * @see org.apache.ftpserver.ftplet.UserManager#doesExist(java.lang.String)
     */
    public boolean doesExist(String username) throws FtpException {
        return true;
    }

    /**
     * @see org.apache.ftpserver.ftplet.UserManager#getAdminName()
     */
    public String getAdminName() throws FtpException {
        throw new FtpException("No custom user handling on this instance");
    }

    /**
     * @see org.apache.ftpserver.ftplet.UserManager#getAllUserNames()
     */
    public String[] getAllUserNames() throws FtpException {
        throw new FtpException("No custom user handling on this instance");
    }

    /**
     * Maps a GeoServer user to an ftp {@link User} by means of the provided Spring Security's
     * {@link UserDetailsService}.
     * <p>
     * The user's home directory is set to the root geoserver data dir in the case of administrators
     * or to {@code <data dir>/incoming/<user name>} in case of non administrators.
     * 
     * @see org.apache.ftpserver.ftplet.UserManager#getUserByName(java.lang.String)
     */
    public BaseUser getUserByName(String username) throws FtpException {
        // basic ftp user setup
        BaseUser user = new BaseUser();
        user.setName(username);
        user.setPassword(null);
        user.setEnabled(true);
        // allow writing
        List<Authority> authorities = new ArrayList<Authority>();
        authorities.add(new WritePermission());
        authorities.add(new ConcurrentLoginPermission(Integer.MAX_VALUE, Integer.MAX_VALUE));
        user.setAuthorities(authorities);

        return user;
    }

    /**
     * @return {@code false}
     * @see org.apache.ftpserver.ftplet.UserManager#isAdmin(java.lang.String)
     */
    public boolean isAdmin(final String username) throws FtpException {
        return false;
    }

    /**
     * @throws FtpException
     *             always, operation not supported
     * @see org.apache.ftpserver.ftplet.UserManager#save(org.apache.ftpserver.ftplet.User)
     */
    public void save(User user) throws FtpException {
        throw new FtpException("No custom user handling on this instance");
    }

}
