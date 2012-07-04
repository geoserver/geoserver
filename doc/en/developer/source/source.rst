.. _source:

Source Code
===========

The GeoServer source code is located at https://github.com/geoserver/geoserver.

To clone the repository::

  % git clone git://github.com/geoserver/geoserver.git geoserver
  
To list available branches in the repsitory::

  % git branch
     2.1.x
     2.2.x
   * master

To switch to the stable branch::

  % git checkout 2.2.x
  
Git
---

Those coming from a Subversion or CSV background will find the git learning curve is a steep one.
Luckily there is lots of great documentation around. Before continuing developers should take the 
time to educate themselves about git. The following is some good references material:

* `The Git Book <http://git-scm.com/book/>`_
* `A nice introduction <http://www.sbf5.com/~cduan/technical/git/>`_

Committing
----------

In order to commit to the repository the following steps must be taken:

#. Property configure your git client for cross platform projects. See :ref:`notes <gitconfig>` below.
#. Register for commit access as described :ref:`here <comitting>`.
#. Fork the canonical git repository into your github account.
#. Ensure your local repository is pointing to non-read only url.

.. note::

   The next section describes how the git repositories are distributed for the project and
   how to manage local repository remote references.
   

Repository distribution
-----------------------

Git is a distributed versioning system which means there is strictly no notion of a single 
central repository, but many distributed ones. Including:

* The **canonical** repository located on github that serves as the official authoritative 
  copy of the source code for project
* Developers **forked** repositories that are also hosted on github. These repositories 
  generally contain everything in the canonical repository and as well any feature or
  topic branches an individual developer is working.
* A developers **local** repository that work is actually done in.

Even though there are numerous copies of the repository they are essentially the same because
they have a common history. This is the magic of git. Any repository that interact seamlessly 
with another repository as long as they share a common history.

A local repository contains a number of *remote* references to repositories hosted on github. 
A local repository typically contains the following remote references:
  
* A remote called **origin** that points to the developers forked github repository.
* A remote called **upstream** that points to the canonical github repositories.
* Any number of remotes that point to other developers forked repositories on github. 

To set up a local repository in this manner:

#. Clone your fork of the canonical repository (where "bob" is replaced with your github account name)::

     % git clone git@github.com:bob/geoserver.git geoserver
     % cd geoserver
   
#. Set up a remote pointing to the canonical repository::

     % git remote add upstream git@github.com:geoserver/geoserver.git
    
   Or if your account does not have push access to the canonical repository use the read-only url::
    
      % git remote add upstream git://github.com/geoserver/geoserver.git

#. Set up remotes pointing to any number of developer forks. These remotes are typically always 
   read-only::
   
      % git remote add aaime git://github.com/aaime/geoserver.git
      % git remote add jdeolive git://github.com/jdeolive/geoserver.git


Repository structure
--------------------

A git repository is composed of a number of branches. These branches fall into three categories:

#. **Primary** branches that correspond to major versions of the software
#. **Release** branches that are used to manage releases of the primary branches
#. **Feature** or topic branches that developers typically do development on

Primary branches
^^^^^^^^^^^^^^^^

Primary branches are present in all repositories and correspond to the main release streams of the 
project. These branches consist of:

* The **master** branch that is the current unstable development branch of the project
* The current **stable** branch that is the current stable development branch of the project
* All previously stable branches

For example at present these branches are:

* **master** - The 2.3.x release stream, where unstable development such as major new features take place
* **2.2.x** - The 2.2.x release stream, where stable development such as bug fixing and stable features take place
* **2.1.x** - The 2.1.x release stream, end-of-like and sees no active development

Release branches
^^^^^^^^^^^^^^^^

Release branches are used to manage releases of stable branches. For each stable primary branch there is a 
corresponding release branch. At present this includes:

* **rel_2.2.x** - The stable release branch
* **rel_2.1.x** - The previous stable release branch

Release branches are only used during a versioned release of the software. At any given time a release branch
corresponds to the exact state of the last release from that branch. During released these branches are tagged.

Release branches are also present in all repositories.

Feature branches
^^^^^^^^^^^^^^^^

Feature branches are what developers use for day to day development. This can include smaller scale bug fixes or 
major new features. Feature branches serve as a staging area for work that allows a developer to freely commit to
them without affecting any of the stable primary branches. For this reason feature branches generally only live
in a developers local repository, and possibly their remote forked repository. Feature branches are not every pushed
up into the canonical repository.

When a developer feels a particular feature is complete enough the feature branch is merged into a primary branch,
usually master. If the work is suitable for the current stable branch the changeset is generally ported back to the
stable branch as well. This is explained in greater detail in the :ref:`` section.

Codebase structure
------------------

Each  branch has the following structure::

  http://svn.codehaus.org/geoserver/
     build/
     doc/
     src/
     data/
     

* ``build`` - release and continuos integration scripts
* ``doc`` - sources for the user and developer guides 
* ``src`` - java sources for GeoServer itself
* ``data`` - a variety of GeoServer data directories / configurations

.. _gitconfig:

Git client configuration
------------------------

When a repository is shared across different platforms it is necessary to have a 
strategy in place for dealing with file line endings. In general git is pretty good about
dealing this without explicit configuration but to be safe developers should set the 
``core.autocrlf`` setting to "input"::

    % git config --global core.autocrfl input

The value "input" essentially tells git to respect whatever line ending form is present
in the git repository.

.. note::

   It is also a good idea, especially for Windows users, to set the ``core.safecrlf`` 
   option to "true"::

      % git config --global core.safecrlf true

   This will basically prevent commits that may potentially modify file line endings.

Some useful reading on this subject:

* http://www.kernel.org/pub/software/scm/git/docs/git-config.html
* https://help.github.com/articles/dealing-with-line-endings
* http://stackoverflow.com/questions/170961/whats-the-best-crlf-handling-strategy-with-git

Development workflow
--------------------

This section contains a number of workflow examples a developer will typically use on a day to day basis. In order 
to understand these examples it is crucial to understand the various phases that a changeset goes though in a git
workflow. The lifecycle of a single change set is as follows:

#. Change is made in a developers local repository.
#. The change is then **staged** for commit. 
#. The staged change is then **committed**.
#. The committed changed is then **pushed** up to a remote repository

All changes don't necessarily have to follow this exact workflow, there are many variations. For instance it is 
common to make many local commits and then push them all up in batch to a remote repository.

Updating from canonical
^^^^^^^^^^^^^^^^^^^^^^^

Generally developers always work a recent version of the official source code. The following example will pull
down the latest changes from the canonical repository on the master branch::

  % git checkout master
  % git pull upstream master
  
And similarly for the stable branch::

  % git checkout 2.2.x
  % git pull upstream 2.2.x

Making local changes
^^^^^^^^^^^^^^^^^^^^

As mentioned above with git there is a two-phase change workflow in which first changes are made and committed 
locally. For example, changing a single file::

  % git checkout master
  # do some work on file x
  % git add x
  % git commit -m "commit message" x
  
The above example included both the staging of a changed file and the committing of it. AGain there are many 
variations but generally the staging process involves using ``git add`` to stage files that have been added 
or modified, and ``git rm`` to stage files that have been deleted. ``git mv`` is also used to move files (and
stage the changes) in one step.
  
Pushing changes to canonical
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Once a developer has made some local commits they generally will want to push them up to a remote repository.
For the primary branches these changes should always be pushed up to the canonical repository. If they are for
some reason not suitable to be pushed to the canonical repository then the work shouldn't be done on a primary
branch, but on a feature branch. 

For example, pushing a local bug fix up to canonical master::
  
  % git checkout master
  # make a change
  % git add/rm/mv ...
  % git commit -m "making change x"
  % git pull upstream master
  % git push upstream master
  
The example shows the practice of first pulling from canonical before pushing to it. Developers should **always** do 
this. Actually if there are changes pending in canonical that you have yet to pull down git will by default not allow 
you to push the commit until you have pulled down those changes.

.. note:: 
   
   A **merge commit** occurs when one branch is merged with another. This includes merging a remote branch with its corresponding
   local branch. A merge commit occurs when two branches are merged and the merge is not a "fast forward" merge. Fast forward
   merges are described `here <http://git-scm.com/book/en/Git-Branching-Basic-Branching-and-Merging>`_ and are worth reading 
   about. An easy way to avoid merge commits is to do a "rebase" when pulling down changes::
   
     % git pull --rebase upstream master
     
   The rebase essentially makes it so your local changes appear in git history after the changes you are pulling down which 
   allows the merge to be a fast forward one. This is not a required practice, merge commits are harmless, but when they 
   occur excessively they can clutter up history making logs harder to read.
   
Working with feature branches
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

As mentioned before it is always a good idea to work on a feature branch and not directly on a primary branch. A classic
situation every developer who has used a version control system has run into is one that occurs when a developer has 
worked on a new feature locally, made a tone of changes but then needs to switch context to work on some other feature or 
bug fix. The developer tries to do that in the midst of the other changes and ends up committing a file they never intended
to. Feature branches are the remedy for this.

To create a new feature branch off of the master branch::

  % git checkout -b my_feature master
  % # make some changes
  % git add/rm, etc...
  % git commit -m "first part of my_feature"
  
Rinse, wash, repeat. The nice about thing this work being on a feature branch is that it is easy to switch context
to work on something else. Just switch back to whatever other branch you need to work on, which will be in a clean
state and then return to the feature branch.

Merging feature branches
^^^^^^^^^^^^^^^^^^^^^^^^

Once a developer is done with a feature branch it must be merged into one of the primary branches and pushed up
to the canonical repository. The way to do this is with the ``git merge`` command::

  % git checkout master
  % git merge my_feature

It's as easy as that. After the feature branch has been merged into the primary branch push it up as described before::

  % git pull --rebase upstream master
  % git push upstream master
  

Porting changes among primary branches
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

More often than not a single change like a bug fix has to be committed to multiple branches. Unfortunately primary
branches can **not** be merged with the ``git merge`` command. Instead we use ``git cherry-pick``.

As an example consider making a change to master::

  % git checkout master
  % # make the change
  % git add/rm/etc... 
  % git commit -m "fixing bug GEOS-XYZ"
  % git pull --rebase upstream master
  % git push upstream master
  
And we want to backport the bug fix to the stable branch as well. To do so we have to note the commit
id of the change we just made on master. The ``git log`` command will do this. Let's assume the commit
id is "123". Backporting to the stable branch then becomes::

  % git checkout 2.2.x
  % git cherry-pick 123
  % git pull --rebase upstream 2.2.x
  % git push upstream 2.2.x

Cleaning up feature branches
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Consider the following situation. A developer has been working on a feature branch and continually gone back 
and forth to and from it making commits here and there. The result is that the feature branch has accumulated
a number of commits on it. But all the commits are really related and what we want is really just one commit.

This is easy with git and you have two options:

#. Do an interactive rebase on the feature branch
#. Do a "squashing" merge

Interactive rebase before commit
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Rebasing allows us to rewrite the commits on a branch, deleting commits we don't want, or merging commits that should
really be done. You can read more about interactive rebasing `here <http://git-scm.com/book/en/Git-Tools-Rewriting-History#Changing-Multiple-Commit-Messages>`_. 

.. warning::

   Much care should be taken with rebasing. You should never rewrite commits that are public, that is commits that are 
   not only in your local repository. Rebasing public commits changes branch history and results in the ability to merge
   with online repositories.
   

An example of an interactive rebase::

  % git checkout my_feature
  % git log

Git log shows the current commit at the top of the branch is commit "123". Then we make 
some changes and commit the result::

  % git commit "fixing bug x" # results in commit 456

Then we realized we forgot to stage a change before committing. So we add the file and commit::

  % git commit -m "oops, forgot to commit that file" # results in commit 678

Again we made a mistake, a typo, so we fix and commit again::

  % git commit -m "darn, made a typo" # results in commit #910

At this point we made three commits when what we really wanted was one. So we rebase specifying the 
revision before the first first commit::

  % git rebase -i 123
  
The result is an editor that allows us to merge commits together, resulting in a single commit. At this point 
we can merge the cleaned up feature branch into master::

  % git checkout master
  % git merge my_feature

Again, be sure to read up on this feature before attempting to use it. And again, **never rebase a public commit**.

Merging with squash
~~~~~~~~~~~~~~~~~~~

The ``git merge`` command takes an optional option ``--squash`` that basically does the merge but does not commit the result 
to the branch being merged into. This will basically squash all the changes from the feature branch into one change set that
has yet to be committed::

  % git checkout master
  % git merge --squash my_feature
  % git commit -m "implemented feature x"
  
  
More useful reading
-------------------

The content in this section is not intended to be a comprehensive introduction to git. There are many things not covered
that are invaluable to day to day work with git. Some more useful info:

* `10 useful git commands <http://about.digg.com/blog/10-useful-git-commands>`_
* `Git stashing <http://git-scm.com/book/en/Git-Tools-Stashing>`_
* `GeoTools git primer <http://docs.geotools.org/latest/developer/procedures/git.html>`_

  



