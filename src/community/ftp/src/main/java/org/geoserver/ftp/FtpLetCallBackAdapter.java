/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ftp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.sun.org.apache.bcel.internal.generic.ACONST_NULL;

/**
 * Adapts an {@link FTPCallback}s as an {@link Ftplet}
 * 
 * @author groldan
 * @see FtpLetFinder
 * @see FTPServerManager
 */
class FtpLetCallBackAdapter extends DefaultFtplet {

    private final FTPCallback callback;

    public FtpLetCallBackAdapter(final FTPCallback callback) {
        this.callback = callback;
    }

    private UserDetails user(User ftpUser) {
        String username = ftpUser.getName();
        String password = ftpUser.getPassword();
        boolean isEnabled = ftpUser.getEnabled();
        GrantedAuthority[] authorities = new GrantedAuthority[0];
        boolean accountNonExpired = true;
        boolean credentialsNonExpired = true;
        boolean accountNonLocked = true;
        
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(username,
            password, isEnabled, accountNonExpired, credentialsNonExpired, accountNonLocked,
            Arrays.asList(authorities));

        return userDetails;
    }

    /**
     * Notifies the {@link FTPCallback} of file delete requests
     * 
     * @see FTPCallback#onDeleteStart
     */
    @Override
    public FtpletResult onDeleteStart(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String fileName = request.getArgument();
        return toFtpResult(callback.onDeleteStart(user, workingDir, fileName));
    }

    /**
     * Notifies the {@link FTPCallback} of file deletion success
     * 
     * @see FTPCallback#onDeleteEnd
     */
    @Override
    public FtpletResult onDeleteEnd(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String fileName = request.getArgument();
        return toFtpResult(callback.onDeleteEnd(user, workingDir, fileName));
    }

    /**
     * Notifies the {@link FTPCallback} of file upload requests
     * 
     * @see FTPCallback#onUploadStart
     */
    @Override
    public FtpletResult onUploadStart(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String fileName = request.getArgument();
        return toFtpResult(callback.onUploadStart(user, workingDir, fileName));
    }

    /**
     * Notifies the {@link FTPCallback} of file upload success
     * 
     * @see FTPCallback#onUploadEnd
     */
    @Override
    public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String fileName = request.getArgument();
        return toFtpResult(callback.onUploadEnd(user, workingDir, fileName));
    }

    /**
     * Notifies the {@link FTPCallback} of file download requests
     * 
     * @see FTPCallback#onDownloadStart
     */
    @Override
    public FtpletResult onDownloadStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String fileName = request.getArgument();
        return toFtpResult(callback.onDownloadStart(user, workingDir, fileName));
    }

    /**
     * Notifies the {@link FTPCallback} of file download success
     * 
     * @see FTPCallback#onDownloadEnd
     */
    @Override
    public FtpletResult onDownloadEnd(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String fileName = request.getArgument();
        return toFtpResult(callback.onDownloadEnd(user, workingDir, fileName));
    }

    /**
     * Notifies the {@link FTPCallback} of remove directory requests
     * 
     * @see FTPCallback#onRemoveDirStart
     */
    @Override
    public FtpletResult onRmdirStart(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String dirName = request.getArgument();
        return toFtpResult(callback.onRemoveDirStart(user, workingDir, dirName));
    }

    /**
     * Notifies the {@link FTPCallback} of remove directory success
     * 
     * @see FTPCallback#onRemoveDirEnd
     */
    @Override
    public FtpletResult onRmdirEnd(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String dirName = request.getArgument();
        return toFtpResult(callback.onRemoveDirEnd(user, workingDir, dirName));
    }

    /**
     * Notifies the {@link FTPCallback} of make directory requests
     * 
     * @see FTPCallback#onMakeDirStart
     */
    @Override
    public FtpletResult onMkdirStart(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String dirName = request.getArgument();
        return toFtpResult(callback.onMakeDirStart(user, workingDir, dirName));
    }

    /**
     * Notifies the {@link FTPCallback} of make directory success
     * 
     * @see FTPCallback#onMakeDirEnd
     */
    @Override
    public FtpletResult onMkdirEnd(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String dirName = request.getArgument();
        return toFtpResult(callback.onMakeDirEnd(user, workingDir, dirName));
    }

    /**
     * Notifies the {@link FTPCallback} of requests to append content to an existing file
     * 
     * @see FTPCallback#onAppendStart
     */
    @Override
    public FtpletResult onAppendStart(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String fileName = request.getArgument();
        return toFtpResult(callback.onAppendStart(user, workingDir, fileName));
    }

    /**
     * Notifies the {@link FTPCallback} of a finished request to append content to an existing file
     * 
     * @see FTPCallback#onAppendEnd
     */
    @Override
    public FtpletResult onAppendEnd(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String fileName = request.getArgument();
        return toFtpResult(callback.onAppendEnd(user, workingDir, fileName));
    }

    /**
     * Notifies the {@link FTPCallback} of a "upload unique file" request to the current directory.
     * <p>
     * This has effectively the same effect than {@link #onUploadStart(FtpSession, FtpRequest)} for
     * the purposes of this module? See <a
     * href="http://www.nsftools.com/tips/RawFTP.htm#STOU">here</a>
     * </p>
     * 
     * @see FTPCallback#onUploadStart
     */
    @Override
    public FtpletResult onUploadUniqueStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String fileName = request.getArgument();
        return toFtpResult(callback.onUploadStart(user, workingDir, fileName));
    }

    /**
     * Notifies the {@link FTPCallback} that an "upload unique file" to the current directory has
     * finished.
     * <p>
     * This has effectively the same effect than {@link #onUploadEnd(FtpSession, FtpRequest)} for
     * the purposes of this module? See <a
     * href="http://www.nsftools.com/tips/RawFTP.htm#STOU">here</a>
     * </p>
     * 
     * @see FTPCallback#onUploadEnd
     */
    @Override
    public FtpletResult onUploadUniqueEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String fileName = request.getArgument();
        return toFtpResult(callback.onUploadEnd(user, workingDir, fileName));
    }

    /**
     * Notifies the {@link FTPCallback} of a request to rename a file
     * 
     * @see FTPCallback#onRenameStart
     */
    @Override
    public FtpletResult onRenameStart(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String fileName = request.getArgument();
        File renameTo = new File(workingDir, fileName);
        File renameFrom = new File(session.getRenameFrom().getAbsolutePath());
        return toFtpResult(callback.onRenameStart(user, workingDir, renameFrom, renameTo));
    }

    /**
     * Notifies the {@link FTPCallback} that a request to rename a file finished
     * 
     * @see FTPCallback#onRenameEnd
     */
    @Override
    public FtpletResult onRenameEnd(FtpSession session, FtpRequest request) throws FtpException,
            IOException {
        UserDetails user = user(session.getUser());
        File workingDir = workingDir(session);
        String fileName = request.getArgument();
        File renameTo = new File(workingDir, fileName);
        File renameFrom = new File(session.getRenameFrom().getAbsolutePath());
        return toFtpResult(callback.onRenameEnd(user, workingDir, renameFrom, renameTo));
    }

    private static FtpletResult toFtpResult(CallbackAction action) {
        if (action == null) {
            return FtpletResult.DEFAULT;
        }
        switch (action) {
        case CONTINUE:
            return FtpletResult.DEFAULT;
        case DISCONNECT:
            return FtpletResult.DISCONNECT;
        case SKIP:
            return FtpletResult.SKIP;
        default:
            throw new IllegalArgumentException("Unknown FTP Callback action: " + action);
        }
    }

    /**
     * Extracts the working directory of the current session to an absolute file system path
     * (contrary to what {@link FileSystemView#getWorkingDirectory() returns, which is relative to
     * the user's home dir})
     */
    private File workingDir(FtpSession session) throws FtpException {
        FtpFile workingDirectory = session.getFileSystemView().getWorkingDirectory();
        String home = session.getUser().getHomeDirectory();
        String absolutePath = workingDirectory.getAbsolutePath().substring(1);
        File workingDir = new File(new File(home), absolutePath);
        return workingDir;
    }

    /**
     * Overrides to disallow SITE commands, so no server administration can be done through the FTP
     * protocol (like managing users,
     */
    /*
     * @TODO: REVISIT public FtpletResult onSite(FtpSession session, FtpRequest request) throws
     * FtpException, IOException { return FtpletResult.SKIP; }
     */
}
