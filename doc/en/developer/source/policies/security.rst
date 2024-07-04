.. _security_procedure:

Security Procedure
==================

This page covers some of the lines of communication and tools available to enact our public security policy (see `SECURITY.md <https://github.com/geoserver/geoserver/blob/main/SECURITY.md>`__).

Vulnerability reporting and discussion
--------------------------------------

Our security policy asks the community to take care in the reporting and discussion of security vulnerabilities:

 **Reporting a Vulnerability**
 
 If you encounter a security vulnerability in GeoServer, please take care to report it in a responsible fashion:
 
 1. Keep exploit details out of public mailing lists and the Jira issue tracker.
 
 2. There are two options to report a security vulnerability:
 
    * To report via email:
 
      Please send an email directly to the volunteers on the private `geoserver-security@lists.osgeo.org <geoserver-security@lists.osgeo.org>`__ mailing list. Provide information about the security vulnerability you might have found in your email.
 
      This is a moderated list: send directly to the address; your email will be moderated; and eventually shared with volunteers.
 
    * To report via GitHub:
 
      Navigate to `security <https://github.com/geoserver/geoserver/security>`_ page, use link for Private vulnerability reporting.
 
      For more information see `GitHub documentation <https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability#privately-reporting-a-security-vulnerability>`_.
 
 3. There is no expected response time. Please be prepared to work with geoserver-security email list volunteers on a solution.
 
 4. Keep in mind participants are volunteering their time, an extensive fix may require fundraising/resources.

 For more information see `Community Support <http://geoserver.org/comm/>`_.

Guidance on communication challenges
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* Issue tracker: If an issue is opened with a security vulnerability, send the link to SECURITY.md to the reporter and close the issue.

* Public Email: If a geoserver-devel or geoserver-user email comes in reporting the vulnerability (or discussing a vulnerability), link to the SECURITY.md file and invite parties to a discussion on the geoserver-security email list privately.
  
* Private Email: If you are contacted directly, share the SECURITY.md, and link to commercial support providers (which may include your employer of course).

* Social media: Ask parties to respect the coordinated vulnerability disclosure policy.

  Recognize that such communication is outside our control and do not further engage. 
  
* Public CVE: Most reliable response is to locate the CVE on advisory database and issue a pull-request clarification.
  
  * https://github.com/advisories?query=geoserver
  * https://github.com/advisories?query=geotools
  
  You may also go through the steps of "disputing" a CVE, this involves contacting the original numbering authority (each of which have their own procedures).
  
  Keep in mind that security researchers may have obligations to report to their national numbering authority.

  Ask that they withhold details and work within our SECURITY.md policy (key phrase "coordinated vulnerability disclosure").
  
Kindness:

* This is a long game, so prioritize sustainability and our geoserver-security volunteers.
* Recognize that the reporter is likely stressed, and may not have much flexibility due legal obligations around vulnerability disclosure.
* This is a sustainability challenge, so do not be shy about seeking sponsorship.

geoserver-security list participation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Participation in the geoserver-security, like commit access, is volunteer based and reflects trust.

* To volunteer, email geoserver-security@lists.osgeo.org stating your interest.

* If you are unable to participate, send an email to step down.

  We understand that short term commitments can get in the way of sustainability initiatives – you are welcome to rejoin at any time.
  
  We will also retire inactive members as needed.

When contacted by an external party on geoserver-security email list:

1. Engage with the reporter as normal to determine if the issue is reproducible, and assess vulnerability.

   * For known issues we can add the reporter to an existing CVE, and indicate the assessment for their review.

   * For new issues we can work with the reporter, and create a CVE (as below), and assess the vulnerability with the reporter's input.

2. The same approach is used for "triage" reports made directly to the security advisory list.

3. The policy of the reporter working with the geoserver-security volunteers remains steadfast.

   Especially with respect to mitigation and scheduling a fix we need to gather interested parties, including the reporter, with time/resources to address the issue.

   Reporters expecting a vendor relationship are invited to contact our service providers (who are very nice).

Coordinated vulnerability disclosure
------------------------------------

GeoServer has adopted a coordinated vulnerability disclosure model, as outlined in `SECURITY.md <https://github.com/geoserver/geoserver/blob/main/SECURITY.md>`__:


  **Coordinated vulnerability disclosure**

  Disclosure policy:
  
  1. The reported vulnerability has been verified by working with the geoserver-security list
  2. GitHub `security advisory <https://github.com/geoserver/geoserver/security>`_ is used to reserve a CVE number
  3. A fix or documentation clarification is accepted and backported to both the "stable" and "maintenance" branches
  4. A fix is included for the "stable" and "maintenance" downloads (`released as scheduled <https://github.com/geoserver/geoserver/wiki/Release-Schedule>`__, or issued via emergency update)
  5. The CVE vulnerability is published with mitigation and patch instructions

  This represents a balance between transparency and participation that does not overwhelm participants. Those seeking greater visibility are encouraged to volunteer with the geoserver-security list; or work with one of the `commercial support providers <https://geoserver.org/support/>`__ who participate on behalf of their customers.

Working with vulnerability reports
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

1. When working with a "triage" or "draft" vulnerability:

   * Invite reporter to participate, they are part of the team addressing the issue,
   * Give credit to the reporter and anyone else involved using the allocated fields
   * Do not immediately request a CVE, and be sure to confirm the vulnerability (for example with a proof of concept) first.
   * A vulnerability flagged from a dependency scan does not automatically indicate that an exploit is available in GeoServer.
   * For popular concerns (like spring-framework upgrade) add each reporter to same advisory.
   
   Be aware that a security researcher may only be tasked with reporting the issue, and might be unavailable
   once you have created a CVE.

2. Preparing report:

   * Package: Always report `org.geoserver.web:gs-web-app` as `geoserver.war` as a useful way to document that the `geoserver.war` includes other jars
   * Affected versions: It is difficult to communicate version ranges, due to limitations in CVE advisory processing, requiring multiple lines.
   
   .. figure:: img/cve-version-range.png
      
      Package guidance and version range

3. Work on providing a fix, mitigation instructions, or best-practice clarification for documentation.
   
   * The use of GitHub private repository associated with an advisory should be used with caution.
   
     If making extensive code changes keep in mind that automations, including QA automations, are not available to be run as part of the pull-request review process.
     
     This may be appropriate when updating documentation / best practice information.
     
     The key advantage is several pull-requests can be managed at once, and merged at the same time as disclosure (see below).
   
   * If you wish to work on a public pull-request (to take advantage of workflow automations) take care that test-cases, commit messages, and documentation updates do not immediately reveal the vulnerability.
   
   * Although not ideal, it is possible to resolve some security issues by documenting a best-practice in the production consideration of the user guide.

4. Request a CVE from GitHub.

   This requires an external review as they check that the details provided are complete.
   
   .. note:: Example: The report "GHSA-cqpc-x2c6-2gmf" has been assigned CVE-2023-41339 and is shown as "not yet published".
   
      .. figure:: img/cve-not-yet-published.png
         
         CVE-2023-41339 Not Yet Published
      
3. Assign a placeholder Jira issue with ``Vulnerability`` category.
   
   * Mentioning the CVE is fine, even if it is not yet public, it will still show up in the database as reserved.
   
   .. note:: Example: The Jira issue GEOS-11121 ticket is created for CVE-2023-41339.
   
      .. figure:: img/cve-issue.png
      
         Jira GEOS-11121 Placeholder

4. During the release process list CVE in "Security Considerations" section of release announcements.
   
   * Initially this lists CVE numbers, indicating a fix is included but does not provide any details
   
   * An indication of the severity is provided to encourage community to update.
   
   * You may change the wording of the recommendation to "recommended" or "essential" or "urgent" as you see fit.
   
   .. note:: It is our policy not to provide details at this time. Any deeply concerned parties can volunteer on the geoserver-security email list, or arrange a vendor relationship with a service provider.

5. Disclosure:
   
   * Wait until the vulnerability has been addressed, for BOTH the stable and maintenance versions, before publishing.
   
   * Update prior release announcements, and placeholder Jira issue, with the complete title of the vulnerability.
  
      .. note:: Example: Security considerations section showing a mix of disclosed and not yet disclosed (no hyperlink) vulnerabilities.
     
         .. figure:: img/cve-disclosure.png
        
            Release announcement communication
   
   * Publish the security advisory to make the vulnerability public
   
   * If you feel a statement is necessary, you may write an appropriate blog post.
     
        .. note:: Example: Statement on covering `Jiffle and GeoTools RCE vulnerabilities <https://geoserver.org/vulnerability/2022/04/11/geoserver-2-jiffle-jndi-rce.html>`__.

Publicly reported issue
^^^^^^^^^^^^^^^^^^^^^^^

When a national agency or similar has already reported a vulnerability publicly, it can be found in the GitHub security advisory database:

1. Locate the issue on https://github.com/advisories?query=geoserver

   .. note:: Example: Public reported CVE-2023-35042 is listed here https://github.com/advisories/GHSA-59x6-g4jr-4hxc
   
2. Create a pull request to revise the issue with useful details such as:

   * maven
   * org.geoserver
   * version
   
   .. note:: Example: CVE-2023-35042 correction https://github.com/github/advisory-database/pull/2721

3. Optional: Work with original agency to try and revise their record.

   .. note:: Example:
      
      A request to mark `CVE-2023-35042 <https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-35042>`__ as duplicate that had been fixed in all supported versions came out as:

       [DISPUTED] GeoServer 2, in some configurations, allows remote attackers to execute arbitrary code via java.lang.Runtime.getRuntime().exec in wps:LiteralData within a wps:Execute request, as exploited in the wild in June 2023. NOTE: the vendor states that they are unable to reproduce this in any version.
   
      This is the opposite of controlling the message, it now appears as if the issue being disputed - rather than accepted as already solved please update etc...

4. Claim the ticket with a Jira issue, linking to the revised GitHub record, or national record as appropriate.
   
   .. note:: Example: CVE-2023-35042 reported to our issue tracker as GEOS-11027
   
      .. figure:: img/cve-issue-public.png
         
         GEOS-11027 documenting state of CVE-2023-35042
