/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.report.impl;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.geoserver.taskmanager.report.Report;
import org.geoserver.taskmanager.report.ReportService;
import org.geotools.util.logging.Logging;

/**
 * Sends a report over email (SMTP).
 *
 * @author Niels Charlier
 */
public class EmailReportServiceImpl implements ReportService {

    private static final Logger LOGGER = Logging.getLogger(EmailReportServiceImpl.class);

    private Filter filter = Filter.ALL;

    private String to;

    private String from;

    private String host;

    public EmailReportServiceImpl(String host, String from, String to) {
        this.to = to;
        this.from = from;
        this.host = host;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public void sendReport(Report report) {
        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.setProperty("mail.smtp.host", host);

        // Get the default Session object.
        Session session = Session.getDefaultInstance(properties);

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Set Subject: header field
            message.setSubject(report.getTitle());

            // Now set the actual message
            message.setText(report.getContent());

            // Send message
            Transport.send(message);

        } catch (MessagingException e) {

            LOGGER.log(Level.WARNING, "Failed to send email report", e);
        }
    }
}
