.. _psc:

Project Steering Committee
==========================

Welcome to the GeoServer organizational system. As with any open source project, we start with people.

Summary
-------

This document describes the role and responsibilities of the Project Steering Committee, as well as the process under which it operates. Much of the definition and inspiration for the GeoServer PSC is taken from the `MapServer Technical Steering Committee <http://mapserver.gis.umn.edu/development/rfc/ms-rfc-1/>`_ and the `Plone foundation <http://plone.org/products/plone/roadmap>`_.

The committee is made up of individuals based on merit irrespective of organization ties.

Structure
---------

The PSC is made up of individuals who are intended to represent the various communities which have a stake in GeoServer. An odd number is chosen to facilitate the voting process and help prevent ties. However, even with an odd number, the voting system may still allow for a tie in some cases. For this reason the PSC has an appointed Chair, whose sole responsibility is to break ties among the PSC.

Turnover is allowed and expected to accommodate people only able to become active on the project in intervals. A PSC member may step down at any time.

Current PSC
-----------

* Alessio Fabiani
* Andrea Aime
* Ian Turton
* Jody Garnett
* Jukka Rahkonen
* Kevin Smith
* Nuno Oliveira
* Simone Giannecchini
* Torben Barsballe

We would like to thank prior PSC members:

* Rob Atkinson
* Justin Deoliveira
* Chris Holmes
* Brent Owens
* Gabriel Roldan
* Phil Scadden
* Christian Mueller
* Ben Caradoc-Davies
* Brad Hards

PSC Membership
--------------

New PSC members
^^^^^^^^^^^^^^^

A new PSC member can be nominated at any time.  Voting for a new PSC is done by current active PSC members.  There is no hard limit to the number of PSC members, but we want a relatively active PSC.  PSC nominations are generally given in recognition to very significant contributions to the project.  Membership is open to non-technical people, for example if someone is to make huge advances to the documentation or marketing of GeoServer, for example.  

Since we demand a fairly active PSC, we expect turnover may be high compared to other projects. Initially we aimed to keep it around 7 PSC members but over time, with sufficient reason, we've expanded it.  

Nominated PSC members must recieve a majority of +1 vote's from the PSC, and no -1's.  

PSC Chair is nominated following the same procedures as PSC members.

Stepping Down
^^^^^^^^^^^^^

If you find you cannot make meetings for a month or two, or have been unable to vote on proposals, by all means step aside. Thank you so much for your time, if you want to groom a successor and then nominate them that is cool, but the nomination process still applies.  

If we do not hear from you for six months we will assume you lost, send out a search party and nominate your replacement.  

That is to say, status on PSC is lost if not active at all in a two month period of time.  Of course you can come back on to the PSC if you become active again, but a new nomination procedure will be needed.  

Bootstrapping
^^^^^^^^^^^^^

First a chair is chosen by the current group of "active" committers. The Chair is then removed from the nominee list.

Everyone on the email lists gets 5 votes for PSC,. Once the list is accepted by those nominated, a volunteer will privately gather the votes posting the results. The 7 nominees receiving the most 5 votes will be selected as the PSC.

Dissolution of PSC
^^^^^^^^^^^^^^^^^^

If there are no suitable replacements, the PSC can decide to go down in number.  If the number of active PSC members drops below 5, however, then we may wish to ask the OSGeo Board for assistance. For more information check out the `OSGeo Governance FAQ <http://www.osgeo.org/faq>`_.

Process
-------

The primary role of the PSC is to make decisions relating to project management. The following decision making process is used. It is based on the "Proposal-Vote" system.

GeoServer Improvement Proposals
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A GeoServer Improvement Proposals (GSIPs) is needed for any action that:

  * Has a major effect on others in the GeoServer community; or
  * Will break backwards compatibility; or
  * Change core code

For more on making a proposal see :ref:`gsip`.

.. include:: gsip_voting.txt

Snap Decisions
^^^^^^^^^^^^^^

A GSIP proposal is NOT needed for:

* an improvement that can go in a community module; or
* a bug fix that doesn't rework anything substantially

For minor decisions where feedback might be desired, consult the development list, or raise it in a video meeting (anyone not attending can follow up on the meeting minutes email).  The GeoServer Project recognizes that it is run those doing the work, and wish to avoid high overhead for 'getting things done'.

For these *snap decisions* that are not official GSIP proposals, everyone 'available' (those in the video meeting or who respond to an email within 4 days) are given the power to vote and decide an issue.  The same voting procedure (+1,+0,-0,-1) is used, but any decision that receives a -1 from any party present (even a  new user), should go to a GSIP.

Responsibilities
----------------

Responsibilities of PSC members fall into the following categories:

 #. Operations
 #. Planning

Operations
^^^^^^^^^^

Day to day project management. Duties include:

**Mailing List Participation**

PSC members are expected to be active on both user and developer email lists, subject to open-source mailing list etiquette of course.

*It is a requirement that all PSC members maintain good public visibility with respect to activity and management of the project. This cannot happen without a good frequency of email on the mailing lists.*

.. note::
   
   Our community is subject to both a responsible disclosure policy and a code of conduct; this is the responsibility of all partipants and is not limited to the PSC.*

**Biweekly Video Meeting Attendance**

PSC members are encouraged to attend one of biweekly Skype meetings. Of course this is not always possible due to various reasons. If known in advance that a member cannot attend a meeting it is polite to email the developer list in response to the meeting reminder. No reason need to be given for not attending the meeting.
   
Meetings are a chance to quickly discuss project activities, review difficult pull requests, and cut down on email.

**Community Commitments**

As an Open Source Geospatial Foundation project we have a number of committments:

* Code of Conduct
* OSGeo Officer
* OSGeo Annual Report
* OSGeo Budget

Planning
^^^^^^^^

Long term project management. Duties include:

**Guiding Major Development Efforts**

*PSC members are expected to help guide the major development efforts of the project. This may include deciding which development efforts should receive priority when different efforts are in conflict.*

*The PSC has the right to veto any proposed development efforts.*

*A major development effort which is intended to become part of the core of GeoServer can be proposed by any interested party, PSC, or non PSC. However, the effort must be approved by the PSC before it can begin.*

**Project Policies**

The PSC is responsible for defining project policies and practiced. Examples include:
 
 * Development Practices

   * Code Reviews
   * Intellectual Property
   * Documentation Requirements
   * Commit Access
   * Testing Requirements
   * Branch Culture

 * Release Procedures

   * Frequency 
   * Version numbering
   * Stable vs Maintenance vs R&D
