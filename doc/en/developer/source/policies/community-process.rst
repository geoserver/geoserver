.. _community_process:

Community Process
=================

This document describes the process that the GeoServer community uses to handle
contributions and additions to the project. This process is used to manage 
new features and additions to the project, as well as for short and long term 
project planning.

The GeoServer community process adheres to the following guidelines to ensure 
that it retains open development practices:

* **Transparency** 
  Project decisions are made in an open and transparent way. When a decision is
  made it is clear as to why it was made.

* **Balance**
  Every member of the developer community has an equal voice. Project decisions
  are made by the community as a whole, and not subject to the whims of any one
  single developer or organizations.

Road map
--------

The GeoServer road map is the center of all project planning. It communicates
to the community what is being worked on and when it is planned to be released.
More specifically the road map is a timeline of planned releases. Each release
has a date attached to it and contains a list of features and critical
bug-fixes/improvements that will be implemented for that release.

The road map is structured into thee parts: **short term**, **medium term**, and
**long term**. The short term road map is made up of features scheduled for the
next two releases. The medium term road map is made up of features scheduled for
the remainder the current stable development branch. The long term road map
consists of features scheduled against the next/unstable development branch.

Short term road map
^^^^^^^^^^^^^^^^^^^

The short term road map is the most important part of the overall road map as it
pertains specifically to what is going to be released in the near future. This
road map is updated on a weekly basis.

Items on the short term road map are described in JIRA as new features, 
improvements, or high priority bug fixes.

Medium term road map
^^^^^^^^^^^^^^^^^^^^

The medium term road map is a higher level view of what is to come in the
current unstable development branch (trunk). It is much less finer grained than
the short term road map. Items on the short term road map may or may not show up
in JIRA. Medium term road map items usually take the form of a 
:ref:`GSIP <gsip>` in progress, or one being discussed, or just a 
:ref:`community module <community_modules>` being developed.

Long term road map
^^^^^^^^^^^^^^^^^^

The long term road map is the highest level view of the road map and describes
what is to come in future development branches. Items on the long term road map
are more or less just ideas or future plans. Long term road map items are
usually described on a RnD page until they become a reality.

Maintaining the short term road map
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Being the most important part of the road map, the process for maintaining and
updating the short term road map is well defined. Road map updates occur weekly
and take place on the developer list as follows:

#. At the beginning of the week an email is sent to the developer list with the
   subject line ``#roadmap update <DATE>``. This email is more or less an quick
   overview of the current state of the road map and contains:

   * The next scheduled release date
   * A list of the unresolved road map items

#. Developers (or any community members) may reply to the initial email and 
   provide progress updates on road map items assigned to them. Developers may
   also propose changes and updates to the road map. Examples of such updates 
   include proposing:

   * new features to be added to the road map, see :ref:`adding_features`
   * features which need to be moved back to a future release due to lack of 
     resourcing
   * features which need to be moved back to a future release due to other 
     issues
   * a change to the scheduled release date

#. Developers and community members discuss the proposed changes on the list.
   Those updates which are agreed upon are factored into the road map and it is 
   updated.

.. _adding_features:

Adding features
^^^^^^^^^^^^^^^

During the road map update developers propose new features and improvements to 
be added to the road map. The following are the prerequisites for proposing a 
new feature:

#. The feature has a sponsor. This means either a developer willing to carry out
   the work or a customer who is paying for it
#. The feature has has gone through the :ref:`GSIP <gsip>` process 
   **if necessary**. Whether a feature requires a GSIP is decided by the 
   community when the feature is proposed

After a feature has met the above prerequisites it is assigned to a release on
the road map. The determining factor for what release a feature should be 
assigned to is based on the estimate of the time to implement the feature, and 
the current :ref:`release_cycle` for the branch the feature is being implemented
on. If the time to implement the feature does not allow it to be assigned to the
next release it is scheduled accordingly into a future release.

.. _release_cycle:

Release cycle
^^^^^^^^^^^^^

GeoServer follows a regular release cycle. Usually this cycle is a release 
every month. However once a development branch has become stable the release
cycle drops off to every few months. Similarly on an unstable development branch
the release cycle can be every few months, until the branch becomes stable 
enough for monthly releases.