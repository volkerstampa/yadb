In the last `post </2014/05/testing-recovery.html>`_ I explained how to test
if the state of an akka-persistence_ based application is properly recovered by
replaying the journals after a system restart. This post is about testing the second
way of recovering application state supported by akka-persistence: by using snapshots.

.. _TestingSnapshotRecovery:

Testing Recovery through Snapshots
==================================

In addition to recover application state by replaying the entire journal akka-persistence
supports taking `snapshots <http://doc.akka.io/docs/akka/2.3.2/scala/persistence.html#Snapshots>`_ of
application state and recover from them. This typically decreases recovery time
significantly as the state is not reconstructed command-message by command-message
but rather all at once and only those commands that arrived after the snapshot was taken need
to be replayed message by message.

Enabling Snapshots
------------------
To enable the `ItemActor` to take snapshots we only need a few lines of code:

.. include:: ../scala/de/blogspot/volkersyadb/akkapersistence/ItemActor.scala
    :start-after: // SNAPSHOT BEGIN
    :end-before: // SNAPSHOT END
    :number-lines: 1
    :code: scala

The actor reacts on the custom message `SaveSnapshot` by providing the actor's
current state to `saveSnapshot`. The entire state of the actor is simply modelled
as the case class `ItemActorSnapshot` containing the item-map and the id-counter.
`saveSnapshot` basically *responds* with
`SaveSnapshotSuccess` or `SaveSnapshotFailure` depending on the success of the
operation and the actor can react accordingly. We do not want to go into the
details of handling these properly here, that is why they are simply ignored.

When it comes to recovery of an actor (e.g. after a restart of the application)
and akka-persistence finds a snapshot for a `Processor` it offers that (`SnapshotOffer`)
before replaying messages from the journal that arrived after the snapshot was taken.

Testing Recovery
----------------
Testing recovery from snapshots is in principle pretty similar to testing
recovery from a journal. We have almost the same steps:

* start the application
* modify the application's state by sending corresponding commands
* take a snapshot
* stop the application
* restart the application (and recover from the snapshot)
* verify the application's state by queries

As these steps are almost identical we should try to reuse as much as possible from
the previous test. In fact we can reuse the entire test and just have to take care
that a snapshot is taken before the application is stopped. Let's one more time have a look at
the central method that starts/stops an application in the tests:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationFixture.scala
    :start-after: // WITH ITEM APPLICATION BEGIN
    :end-before: // WITH ITEM APPLICATION END
    :number-lines: 1
    :code: scala

The application is shutdown after the test by invoking the corresponding method
of the application (line 8). If we are able to inject taking a snapshot
here we are basically all set as in this case akka-persistence will find the
snapshot after the next restart and recover from that instead of the
journal. We can actually easily do this as we already amend the application
with some test-extensions and we just have to modify this a bit for taking
a snapshot at shutdown.

First we overwrite the `newItemApplication` method in the trait `WithItemApplicationWithSnapshot`
that extends the original one `WithItemApplication` to amend
the application with a snapshot-specific extension:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationFixture.scala
    :start-after: // WITH SNAPSHOT APPLICATION BEGIN
    :end-before: // WITH SNAPSHOT APPLICATION END
    :number-lines: 1
    :code: scala

This extension overwrites the `shutdown` and also amends the `ItemActor` by overwriting
`itemActorProps`:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationTestExtensions.scala
    :start-after: // SNAPSHOT APPLICATION BEGIN
    :end-before: // SNAPSHOT APPLICATION END
    :number-lines: 1
    :code: scala

`shutdown` simply sends a `SaveSnapshot` message to `ItemActor` and waits for
a response (line 8) before it actually shuts down the application (line 13). However
to make `ItemActor` actually respond to this message it has to be modified slightly
and that is why `itemActorProps` is overwritten as well (line 4) and the returned
actor is extended with the trait `RespondToSnapshotRequest`:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/RespondToSnapshotRequest.scala
    :start-after: // RESPOND TO SNAPSHOT BEGIN
    :end-before: // RESPOND TO SNAPSHOT END
    :number-lines: 1
    :code: scala

The `receive`-method of this trait intercepts `SaveSnapshot` messages and keeps
the sender of the message (line 9) before it continues with normal processing
(line 10). The saved sender reference is used to forward the `SaveSnapshotSuccess` (line 12) or
`SaveSnapshotFailure` (line 16) messages to it.

Armed with this the test for testing successful recovery from snapshots can simply extend
the one for testing recovery from the journal but mix-in the `WithItemApplicationWithSnapshot`-trait:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationRecoverFromSnapshotSpec.scala
    :start-after: // RECOVER FROM SNAPSHOT SPEC BEGIN
    :end-before: // RECOVER FROM SNAPSHOT SPEC END
    :number-lines: 1
    :code: scala

So there is no need to redundantly formulate the individual tests for recovery
after create, update and delete. To verify how those tests are working we
can break the implementation of taking snapshots. We could for example forget
to include the id-counter in a snapshot, so stripping down the `ItemActorSnapshot` to
`case class ItemActorSnapshot(itemById: Map[ItemId, Item])`. Running the tests
immediately shows ...oops... that all are running fine. So the bug created by this
change is not discovered by the tests. It seems we need an additional one. The difference
between recovery from the journal and recovery from a snapshot is that when the state
is recovered from the journal the same application logic is triggered as in case
of normal operation. So if tests have proven that during normal operation the
application's state is not being messed up, its hard to mess it up during recovery
(as long as all command-messages really end up in the journal).
However in case of snapshots this is different as the state is handled independently
from the processed messages and that is why we have to add more tests. The bug
we just introduced resets the id-counter to 0 after each restart. To verify
that this does not occur we need to create a new item before and after the
restart and check if both can be retrieved afterwards. The test looks like this:

.. include:: ../../test/scala/de/blogspot/volkersyadb/akkapersistence/ItemApplicationRecoverFromSnapshotSpec.scala
    :start-after: // RECOVER ID COUNTER BEGIN
    :end-before: // RECOVER ID COUNTER END
    :number-lines: 1
    :code: scala

First an item is created before the restart (line 2) and another one after the
restart (line 7). Then the test asserts that both can be retrieved (line 9-10).
With the bug introduced above this test fails (in line 9) as the newly created
item gets the same id as the old one and thus overwrites it. Fixing the bug
makes the test run fine again.

This blog post showed how one can reuse the tests for recovery from the journal
for testing recovery from snapshots. The next one shows if the same ideas can
even be applied to testing successful recovery from an *old* journal.

.. _akka-persistence: http://doc.akka.io/docs/akka/2.3.2/scala/persistence.html
.. _akka-serialization: http://doc.akka.io/docs/akka/2.3.2/scala/serialization.html
