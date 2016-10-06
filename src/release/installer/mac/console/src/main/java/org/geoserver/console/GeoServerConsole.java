/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.console;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.start.Main;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Swing application which controls a running instance of GeoServer.
 * 
 * @author Justin Deoliveira, OpenGEO
 *
 */
public class GeoServerConsole {

    /**
     * Strategy for controlling a Jetty instance.
     */
    public interface Handler {
        void start() throws Exception;
        boolean isStarted() throws Exception;
        
        void stop() throws Exception;
        boolean isStopped() throws Exception;
    }
    
    /**
     * Handler which runs an internal instance of {@link Server}.
     */
    public static class DebugHandler implements Handler {

        /**
         * the server instance.
         */
        Server server;
        
        public DebugHandler() {
            server = new Server();
            initServer( server );
            
        }
        
        void initServer(Server server ) {
            ServerConnector conn = new ServerConnector(server);
            conn.setPort(Integer.getInteger("jetty.port", 8080));
            
            //conn.setThreadPool(tp);
            conn.setAcceptQueueSize(100);
            server.setConnectors(new Connector[] { conn });

            WebAppContext wah = new WebAppContext();
            wah.setContextPath("/geoserver");
            wah.setWar("src/main/webapp");
            server.setHandler(wah);
            wah.setTempDirectory(new File("target/work"));
        }

        public void start() throws Exception {
            server.start();
        }
        
        public boolean isStarted() throws Exception {
            return server.isStarted();
        }

        public boolean isStopped() {
            return server.isStopped();
        }

        public void stop() throws Exception {
            server.stop();
        }
    }
    
    /**
     * Handler which runs a live jetty instance via {@link Main}. 
     */
    public static class ProductionHandler implements Handler {

        Main main;
        
        public ProductionHandler() throws IOException {
            main = new Main();
        }

        public void start() throws Exception {
            main.start(main.processCommandLine(new String[]{}));
        }
        public boolean isStarted() throws Exception {
            //attempt a connection
            Socket s = null;
            try {
                s = new Socket( InetAddress.getLocalHost(), 8080 );
                return s.isConnected();    
            }
            catch( ConnectException e ) {
                return false;
            }
            finally {
               if ( s != null) s.close();    
            }
        }

        public boolean isStopped() throws Exception {
            return false;
        }

        public void stop() throws Exception {
            main.main( new String[]{"--stop"} );
        }
    }
    
    /**
     * the display
     */
    Frame w;
    /**
     * life cycle handler
     */
    Handler handler;
    /**
     * geoserver data directory
     */
    File dd;
    
    public GeoServerConsole(Handler handler) {
        this.handler = handler;
        
        w = new Frame();
        w.setVisible( true );
        
        dd = findDataDirectory();

        // redirect stdout/stderr to frame
        final OutputStream out = new BufferedOutputStream(new OutputStream() {
            byte[] buf = new byte[1];
            @Override
            public void write(int b) throws IOException {
                buf[0] = (byte) b;
                write(buf, 0, 1);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                final String content = new String(b, off, len);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        w.addText(content);
                    }
                }); 
            }
        }, 100);

        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(out));
    }
    
    File findDataDirectory()  {
        //1. look in the web.xml
        File dd = lookupInWebXml();
        
        //2. look for environment variable
        if ( dd == null ) {
            dd = lookupEnvironmentVariable();
        }
        
        //3. look up java property
        if ( dd == null ) {
            dd = lookupSystemProperty();
        }
        
        //4. look for webapps/geoserver/data 
        if ( dd == null ) {
            dd = lookupWebapps();
        }
        
        if ( dd != null ) {
            return dd;
        }

        throw new IllegalStateException( "Could not find data directory");
    }
    
    File lookupInWebXml( ){
//        File web = new File( "webapps/geoserver/WEB-INF/web.xml" );
//        if ( web.exists() ) {
//            org.w3c.dom.Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( web );
//            NodeList contextParamElements = dom.getElementsByTagName( "context-param" );
//            for ( int i = 0; i < contextParamElements.getLength(); i++ ) {
//                Element contextParamElement = (Element) contextParamElements.item( i );
//                
//                Element paramNameElement = null;
//                Element paramValueElement = null;
//                
//                for ( int j = 0; j < contextParamElement.getChildNodes().getLength(); j++ ) {
//                    Node n = contextParamElement.getChildNodes().item( j );
//                    if ( n instanceof Element && n.getNodeName().equals( "param-name") ) {
//                        Element e = (Element) n;
//                        if ( e.getFirstChild() != null && e.getFirstChild().getTextContent() != null && 
//                            e.getFirstChild().getTextContent().equals( "GEOSERVER_DATA_DIR")) {
//                        
//                            paramNameElement = e;
//                            break;
//                        }
//                    }
//                    if ( n instanceof Element && n.getNodeName().equals( "param-value") ) {
//                        Element e = (Element) n;
//                        if ( e.getFirstChild() != null ) {
//                            paramNameElement = e;
//                            break;
//                        }
//                    }
//                }
//                 
//                if ( paramNameElement != null ) {
//                    for ( int j = 0; j < contextParamElement.getChildNodes().getLength(); j++ ) {
//                        Node n = contextParamElement.getChildNodes().item( j );
//                        if ( n instanceof Element && n.getNodeName().equals( "param-value") ) {
//                            Element e = (Element) n;
//                            if ( e.getFirstChild() != null && e.getFirstChild().getTextContent() != null && 
//                                e.getFirstChild().getTextContent().equals( "GEOSERVER_DATA_DIR")) {
//                            
//                                paramNameElement = e;
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
//        }
        return null;
    }
    
    File lookupSystemProperty() {
        String dd = System.getProperty( "GEOSERVER_DATA_DIR" );
        if ( dd != null ) {
            return makeAbsolute( dd );
        }
        
        return null;
    }
    
    File lookupEnvironmentVariable() {
        String dd = System.getenv( "GEOSERVER_DATA_DIR" );
        if ( dd != null ) {
            return makeAbsolute( dd );
        }
        
        return null;
    }
    
    File lookupWebapps() {
        String dd = "webapps" + File.separator + "geoserver" + File.separator + "data";
        if (  new File( dd ).exists() ) {
            return makeAbsolute( dd );
        }
        
        return null;
    }
    
    File makeAbsolute( String file ) {
        File f = new File( file );
        if ( f.isAbsolute() ) {
            return f;
        }
        
        return new File( System.getProperty( "user.dir" ), file );
    }
    
    class Frame extends JFrame {
        
        JTextArea textArea; 
        JLabel statusLabel;
        JMenuItem startMenuItem,shutDownMenuItem,copyMenuItem;
        
        Frame() {
            initComponents();
        }

        void initComponents() {
            setLayout( new BorderLayout( 5, 5 ) );
            
            textArea = new JTextArea();
            final JScrollPane scrollPane = new JScrollPane( textArea );
            add( scrollPane, BorderLayout.CENTER );
            
            scrollPane.setPreferredSize( new Dimension( 450, 300 ) );
            textArea.setLineWrap( false );
            
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
            
            add( Box.createHorizontalStrut(5), BorderLayout.WEST );
            add( Box.createHorizontalStrut(5), BorderLayout.EAST );
            
            add( Box.createVerticalStrut(5), BorderLayout.NORTH );
            
            //JPanel upperPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
            //add( upperPanel, BorderLayout.NORTH );
            
            //final JCheckBox logCheckBox = new JCheckBox( "Server Log", true );
            //upperPanel.add( logCheckBox );
            
            JPanel statusPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
            add( statusPanel, BorderLayout.SOUTH );
            statusPanel.add( Box.createHorizontalStrut(3));
            
            statusLabel = new JLabel();
            statusPanel.add( statusLabel );
            
            //menu
            JMenuBar menuBar = new JMenuBar();
            setJMenuBar( menuBar );
            
            JMenu geoServerMenu = new JMenu( "Server");
            menuBar.add( geoServerMenu );
            
            JMenu editMenu = new JMenu( "Edit");
            menuBar.add( editMenu );
            
            startMenuItem = new JMenuItem( "Start" );
            geoServerMenu.add( startMenuItem );
            
            startMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        start();
                    } 
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                } 
            });
            
            
            shutDownMenuItem = new JMenuItem( "Shutdown" );
            geoServerMenu.add( shutDownMenuItem );
            shutDownMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        stop();
                    } 
                    catch (Exception e1) {
                        throw new RuntimeException( e1 );
                    }
                }
            });
            shutDownMenuItem.setEnabled( false );
            
            copyMenuItem = new JMenuItem( "Copy" );
            editMenu.add( copyMenuItem );
            copyMenuItem.setEnabled( false );
            copyMenuItem.addActionListener( new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    String selection = textArea.getSelectedText();
                    StringSelection data = new StringSelection(selection);
                    Clipboard clipboard = 
                         Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(data, data);
                }
                
                
            });
            editMenu.addSeparator();
            
            JMenuItem selectAllMenuItem = new JMenuItem( "Select All");
            editMenu.add( selectAllMenuItem );
            
            selectAllMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    textArea.selectAll();
                }
            });
            
            editMenu.addMenuListener( new MenuListener() {

                public void menuCanceled(MenuEvent e) {
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuSelected(MenuEvent e) {
                    String selected = textArea.getSelectedText();
                    copyMenuItem.setEnabled( selected != null && !selected.equals( "" ) );
                }
                
            });
            
            setSize( 550, 350 );
            setTitle( "GeoServer");
            setIconImage( Toolkit.getDefaultToolkit().createImage( GeoServerConsole.class.getResource( "gs.gif")) );
            pack();
            
            addWindowListener(new WindowAdapter(){
                
                public void windowClosing(WindowEvent e) {
                    boolean started = true;
                    try {
                         started = handler.isStarted();
                    } catch (Exception ex) {
                        throw new RuntimeException( ex );
                    }
                    
                    if ( started ) {
                        String msg = "Would you like to shutdown GeoServer before exiting?";
                        switch( JOptionPane.showConfirmDialog( Frame.this, msg ) ) {
                        case JOptionPane.CANCEL_OPTION:
                            //cancel the operation
                            setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
                            return;
                        case JOptionPane.OK_OPTION:
                            setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
                            
                            //wait until server shutdown to close the window
                            Thread t = new Thread( new Runnable() {
                                public void run() {
                                    try {
                                        while( handler.isStarted() ) {
                                            Thread.sleep( 500 );
                                        }
                                        
                                        Frame.this.setDefaultCloseOperation( EXIT_ON_CLOSE );
                                        Frame.this.dispose();
                                        System.exit( 0 );
                                    }
                                    catch( Exception e ) {
                                        throw new RuntimeException( e );
                                    }
                                }
                            });
                            t.start();
                            stop();
                            return;
                            
                        case JOptionPane.NO_OPTION:
                            setDefaultCloseOperation( DISPOSE_ON_CLOSE );
                            return;
                        }
                    }
                    else {
                        setDefaultCloseOperation( EXIT_ON_CLOSE );
                    }
                }
            });
            stopped();
        }
        
        void addText( String text ) {
            // only maintain so much of the log
            if (textArea.getLineCount() > 1000) {
                int d = textArea.getLineCount() - 1000;
                for (int i = 0; i < d; i++) {
                    try {
                        textArea.replaceRange(null, textArea.getLineStartOffset(0), textArea.getLineEndOffset(0));
                    } catch (BadLocationException e) {
                        // this is bad!
                        e.printStackTrace();
                    }
                }
                
            }

            Document d = textArea.getDocument();
            textArea.insert( text, d.getLength() );
            textArea.select( d.getLength(), d.getLength() );
        }
        
        void start() throws Exception {
            starting();
            
            //start a thread to monitor server status
            Thread t1 = new Thread( new Runnable() {
                public void run() {
                    try {
                        while (!handler.isStarted() ) {
                            Thread.sleep( 500 );
                        }
                    } 
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    
                    started();
                }
            }, "startup monitor");
            t1.start();
        
            //start another thread to start the server to avoid blocknig the ui
            Thread t2 = new Thread( new Runnable() {
                public void run() {
                    try {
                        handler.start();
                    } 
                    catch (Exception e) {
                        throw new RuntimeException( e );
                    }
                }
            }, "startup executor");
            t2.start();
        }
        
        void starting() {
            statusLabel.setText( "GeoServer is starting...");
            statusLabel.setIcon( new ImageIcon( getClass().getResource( "starting.png")));
            startMenuItem.setEnabled(false);
        }
        
        void started() {
            statusLabel.setText( "GeoServer is started.");
            statusLabel.setIcon( new ImageIcon( getClass().getResource( "started.png")));
            shutDownMenuItem.setEnabled(true);
            
            try {
                Browser.openUrl( "http://localhost:8080/geoserver/web");
            } 
            catch (IOException e) {
                //log this
                e.printStackTrace();
            }
        }
        
        void stop() {
            stopping();
            
            Thread t1 = new Thread( new Runnable() {
                public void run() {
                    try {
                        while ( !handler.isStopped() ) {
                            Thread.sleep( 500 );
                        }
                        
                        stopped();
                    } 
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }, "shutdown monitor");
            t1.start();
            
            Thread t2 = new Thread( new Runnable() {
                public void run() {
                    try {
                        handler.stop();
                    } 
                    catch (Exception e) {
                        throw new RuntimeException( e );
                    }
                }
            }, "shutdown executor");
            t2.start();
        }
        
        void stopping() {
            statusLabel.setText( "GeoServer is stopping...");
            statusLabel.setIcon( new ImageIcon( getClass().getResource( "stopping.png")));
            shutDownMenuItem.setEnabled(false);
        }
        
        void stopped() {
            statusLabel.setText( "GeoServer is stopped.");
            statusLabel.setIcon( new ImageIcon( getClass().getResource( "stopped.png")));
            startMenuItem.setEnabled(true);
        }
    }
}
