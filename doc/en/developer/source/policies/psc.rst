.. _psc:

Project Steering Committee
==========================

Welcome to the GeoServer organizational system, as with any open source project we start with people.

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
* Brad Hards
* Ian Turton
* Jody Garnett
* Jukka Rahkonen
* Kevin Smith
* Simone Giannecchini

We would like to thank prior PSC members:

* Rob Atkinson
* Justin Deoliveira
* Chris Holmes
* Brent Owens
* Gabriel Roldan
* Phil Scadden
* Christian Mueller
* Ben Caradoc-Davies

PSC Membership
--------------

New PSC members
^^^^^^^^^^^^^^^

A new PSC member can be nominated at any time.  Voting for a new PSC is done by current active PSC members.  There is no hard limit to the number of PSC members, but we want a relatively active PSC.  PSC nominations are generally given in recognition to very significant contributions to the project.  Membership is open to non-technical people, for example if someone is to make huge advances to the documentation or marketing of GeoServer, for example.  

Since we demand a fairly active PSC we expect turnover may be high compared to other projects, so initially we will aim to keep it around 7 PSC members.  But given sufficient reason we will expand that.  

Nominated PSC members must recieve a majority of +1 vote's from the PSC, and no -1's.  

PSC Chair is nominated following the same procedures as PSC members.

Stepping Down
^^^^^^^^^^^^^

If you find you cannot make meetings for a month or two, by all means step aside. Thank you so much for your time, if you want to groom a successor and then nominate them that is cool, but the nomination process still applies.  

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

 * Issues that require a decision are based on GeoServer Improvement Proposals (GSIPs). For more on making a proposal see :ref:`gsip`
 * Proposals may be made by any interested party (PSC, Non-PSC, committer,user,etc...)
 * Proposals should be addressed within one week of being submitted, votes take place at the subsequent IRC meeting.
 * Each PSC member may vote one of the following in support of the proposal:
   
   * +1 : For
   * -1 : Against
   * +0: Mildly for, but mostly indifferent
   * -0: Mildly against, but mostly indifferent

 * A -1 vote must be accompanied with a detailed reason of being against.
 * A vote is *successful* if there is a majority of positive votes.
 * A vote is *unanimous*, if there are either:

   #. No -1 votes against it, or
   #. No +1 votes for it.

 * In the event of an *successful non unanimous* vote, the following steps are taken:
 
   * Each member who votes -1 _may_ supply an alternative with which the original author can use to rework the proposal in order to satisfy that PSC member.
   * If at least one -1 voting PSC member supplies some alternative criteria, the original author must rework the proposal and resubmit, and the voting process starts again from scratch.
   * If no -1 voters are able to supply alternative criteria, the proposal is accepted.
   * In the event of an *unsuccessful* vote, the author may rework and submit. A proposal may not be resubmitted after being rejected three times.
   * Note that a majority of positive votes does not need to be a majority of the full PSC, just a majority of the 'active' PSC - defined by those present at an IRC meeting or responding within 4 days on email.  PSC members need not sound in on every single motion, but are expected to be active - the section on stepping down details that if a PSC is not active for 2 months they will

When not to use the GSIP
^^^^^^^^^^^^^^^^^^^^^^^^

A GSIP is only needed for:

  * an action that has a major effect on others in the GeoServer community.
  * If an action will break backwards compatibility, or change core code, a GSIP is recommended.

A GSIP is NOT needed for:

  * an improvement that can go in a community module; or
  * a bug fix that doesn't rework anything substantially

For minor decisions where feedback might be desired, the course of action to take is to consult the development list or raise it in an irc meeting.  The GeoServer Project recognizes that it is run those who are actually doing the work, and thus we want to avoid high overhead for 'getting things done'.

.. note:: Snap Decisions

   For all decisions that are not official GSIP proposals, those 'present' (those in the Skype meeting or who bother to respond to an email within 4 days) are given the power to vote and decide an issue.  The same voting procedures are used, but any vote that meets a -1 from any party present (even a  new user), should go to a GSIP.  

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

   Biweekly Skype Meeting Attendance

   PSC members are encouraged to attend one of biweekly Skype meetings. Of course this is not always possible due to various reasons. If known in advance that a member cannot attend a meeting it is polite to email the developer list in response to the meeting reminder. No reason need to be given for not attending the meeting.
   
   Meetings are a chance to quickly discuss project activities, review difficult pull requests, and cut down on email.

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
   * Stable vs R&D
