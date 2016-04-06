.. _gsip:

GeoServer Improvement Proposals
===============================

GeoServer Improvements Proposals (GSIP) are the formal mechanism used to manage major changes to GeoServer in a responsible fashion. Examples of changes which are managed by
the GSIP process include:

* major features
* code re-architecture
* community process improvements
* upgrading key dependencies
* intellectual property

How a GSIP works
----------------

The typical life cycle of a GSIP is as follows:

#. Developer has an intent to perform a major change
#. Developer communicates with the community about the change
#. Developer goes off and implements the change
#. Developer writes a GSIP and presents it to the community for feedback
#. The PSC votes on the GSIP
#. Developer commits changes upon receiving a positive vote

Writing a GSIP
--------------
   
GSIPs are written up on the 
`Proposals <https://github.com/geoserver/geoserver/wiki/Proposals>`_ wiki page.

If you have write access login and follow the steps below. If you do not have write access GitHub will automatically create a fork for you as you edit individual pages.

To make a GSIP:

#. Navigate to the wiki: https://github.com/geoserver/geoserver/wiki/
#. Select and copy the proposal template to your clipboard. (Template is available `here <https://github.com/geoserver/geoserver/wiki/GSIP-XXXX>`_)
#. Navigate to the proposals page: https://github.com/geoserver/geoserver/wiki/Proposals
#. Edit the page with a new link under *Proposals Under Discussion*:
   
   ```
   [GSIP 116 - Use Apache Style Contributor Agreement](GSIP 116)
   ```
 
   #. The number should be the next "available" GSIP number.
   #. The title should be short and descriptive

#. Save your change to the *Proposal* page, and click on your new link to create a new page.
#. Click on your new link to create the page, fill in the page contents by pasting the proposal page template from your clipboard.
#. Fill in the information in the page template, and click ``Save`` when
   complete.
   
Voting on a GSIP
----------------

One of the duties of the GeoServer Project Steering Committee is to vote on 
GSIPs. The voting process works as follows:

* Each active PSC member gets a single vote, which can be one of "+1", "+0", "0", "-0", or "-1":

    * A vote of "+1" means support for a proposal

    * The neutral votes "+0", "0", and "-0" all count the same as "0" but permit PSC members
      to indicate a slight tendency for, neutral, or against a proposal respectively

    * A vote of "-1" means opposition to  a proposal

* Voting remains open for seven days or until all active PSC members have cast their vote

* Any active PSC member can during these seven days request an extension of voting for
  an additional seven days; the voting period can only be extended once

* Any PSC member who votes "-1" against a proposal must provide a
  reasonable explanation as to why

* Any PSC member who votes "-1" against a proposal has a limited time to
  provide constructive feedback as to how the vote can be turned

* The GSIP author must incorporate any reasonable feedback into the proposal

* PSC members who voted "-1" consider whether their feedback has been addressed
  and whether they wish to change their vote

* Only active PSC members are counted for determining vote percentages

* A GSIP is accepted if it receives:

    * at least 30% "+1" votes, and

    * a quorum of at least 50% "+1"/"+0"/"0"/"-0" votes, and

    * no "-1" votes (or all feedback from any "-1" votes has been addressed
      and any "-1" voters have changed their votes)

GSIP FAQ
--------

Q. When do I need to make a GSIP?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A: Generally you will make a GSIP when you want something major done in
GeoServer and need feedback from others. GSIPs are needed for things that will
have an impact on other community members, and thus should be talked about.

GSIPs are not intended to be a way to get feedback on experimental work. There
are alternate mechanisms for this such as creating an R&D page on the wiki, 
svn branches and spikes, etc... Often once the idea is formalized through 
experimentation it can be turned in to an official GSIP.

Q. Who can comment on a GSIP?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A: Anyone and everyone can comment on a GSIP including regular users, and it is
encouraged. Comments an take place on the email lists, or as comments on the
wiki page for the GSIP itself. Feedback from the user community is definitely
desired so that the developers do not just making decisions in a vacuum. If you
speak up your voice will be heard.

Q. Who can vote on a GSIP?
^^^^^^^^^^^^^^^^^^^^^^^^^^

A: Only PSC members can officially vote on a GSIP. But more often than not this
vote is based on general feedback from the user community. 

Q: What happens if I propose a GSIP but I don't have the time to implement it?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A: If a proposal is made but is not completed then it will be taken from a
release schedule and live as a deferred proposal. A deferred proposal can be
picked up by the original developer or another developer, or it can live in 
limbo until it officially gets rejected.

Q: If I am a PSC member and I make a GSIP, do I still vote on it?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A: Yes.
