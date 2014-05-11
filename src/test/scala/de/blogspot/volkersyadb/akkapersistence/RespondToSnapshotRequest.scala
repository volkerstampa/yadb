package de.blogspot.volkersyadb.akkapersistence

import akka.actor.{ActorRef, Actor}
import akka.persistence._
import de.blogspot.volkersyadb.akkapersistence.ItemActor.SaveSnapshot

// RESPOND TO SNAPSHOT BEGIN
trait RespondToSnapshotRequest extends Actor {

  private var lastSnapshotSender: Option[ActorRef] = None

  abstract override def receive: Receive = respondToSnapshotReceive.orElse(super.receive)

  def respondToSnapshotReceive: Receive = {
    case SaveSnapshot =>
      lastSnapshotSender = Some(sender())
      super.receive(SaveSnapshot)

    case message: SaveSnapshotSuccess =>
      super.receive(message)
      respondToSnapshotRequester(message)

    case message: SaveSnapshotFailure =>
      super.receive(message)
      respondToSnapshotRequester(message)
  }

  private def respondToSnapshotRequester(response: AnyRef) = {
    lastSnapshotSender.foreach(_ ! response)
    lastSnapshotSender = None
  }
}
// RESPOND TO SNAPSHOT END
