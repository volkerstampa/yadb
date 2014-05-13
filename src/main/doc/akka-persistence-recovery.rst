In the last `post <../04/akka-persistence-and-testing.html>`_
I introduced the problem area of what and how to test applications
that use akka-persistence_ for persisting their state. I demonstrated the first
important test verifying that the custom serializers for the messages to be
persisted are actually used. This second part is about testing the successful
recovery of the application state after a restart.

.. _TestingRecovery:

Testing Recovery
================

To test recovery we basically have to execute the following steps:

* start the application (with an empty journal)
* modify the application's state by sending corresponding commands
* stop the application
* restart the application
* verify the application's state by queries

While it sounds a little bit odd to start, stop and restart an application in a
unit test, with the ``ItemApplicationFixture`` that we have seen in the last post
it is actually not a big deal. Let's have another quick look at the central method:
``withApplication``

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationFixture.scala
    :start-after: // WITH ITEM APPLICATION BEGIN
    :end-before: // WITH ITEM APPLICATION END
    :number-lines: 1
    :code: scala

As you can see it takes a *temporary* folder (where journals and snapshots are stored)
as one argument (``persistDir``) and test-code (``block``) as second argument.
The application is started (line 7),
the test is executed and in any case (failure or success) the application is shut down (line 8).

Armed with this we can pretty easily start, stop and restart the application in a test,
like follows:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationRecoverSpec.scala
    :start-after: // RECOVER CREATE BEGIN
    :end-before: // RECOVER CREATE END
    :number-lines: 1
    :code: scala

This test starts the application (line 1), creates a new item (line 2) and
shuts the application down by ending the block.
Immediately after that it is restarted (line 4). As the directory
for storing the journal is the same as before (``persistDir``) this should
recover the state of the application from the previously written journal
such that the following ``findItem`` successfully
returns the item created before (line 5-6).

The application that is passed to the block
is extended with some convenience functions and also with a wrapper for the
``ItemService`` that eases invoking item-commands by waiting for the returned ``Future``\s.
Let's have a quick look at ``createNewItem``\:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemServiceTestExtensions.scala
    :start-after: // CREATE NEW ITEM BEGIN
    :end-before: // CREATE NEW ITEM END
    :number-lines: 1
    :code: scala

And the implementation of ``successOf`` looks like follows:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/TestUtil.scala
    :start-after: // TEST HELPER BEGIN
    :end-before: // TEST HELPER END
    :number-lines: 1
    :code: scala

Factoring out this kind of code required to handle the ``Future`` or ``Try`` avoids polluting the
test details that do not contribute to the documentation aspect of the test, as the test does not
test the creation of new items, but rather just the proper recovery.

Similar tests exist for verifying that an update is recovered successfully:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationRecoverSpec.scala
    :start-after: // RECOVER UPDATE BEGIN
    :end-before: // RECOVER UPDATE END
    :number-lines: 1
    :code: scala

or a delete:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationRecoverSpec.scala
    :start-after: // RECOVER DELETE BEGIN
    :end-before: // RECOVER DELETE END
    :number-lines: 1
    :code: scala

We can easily verify that these tests are actually significant by introducing a bug
in our code. For example, if we *forget* to wrap the ``UpdateItem`` command in a
``Persistent`` when we send it in ``ItemService``\:

.. include:: ../../main/scala/de/blogspot/volkersyadb/akkapersistence/ItemService.scala
    :start-after: // ITEM SERVICE UPDATE BEGIN
    :end-before: // ITEM SERVICE UPDATE END
    :number-lines: 1
    :code: scala

as well as when we receive it in ``ItemActor``\:

.. include:: ../../main/scala/de/blogspot/volkersyadb/akkapersistence/ItemActor.scala
    :start-after: // ITEM ACTOR UPDATE BEGIN
    :end-before: // ITEM ACTOR UPDATE END
    :number-lines: 1
    :code: scala

the corresponding test fails with::

  Some(Item(1,2 - description)) was not equal to Some(Item(1,3 - description))

This completes the tests for application-state recovery from the journal after
a restart. What we still do not know is, if the application state can also be
recovered from a snapshot. We will have a closer look into this in the next
blog-post.

.. _akka-persistence: http://doc.akka.io/docs/akka/2.3.0/scala/persistence.html