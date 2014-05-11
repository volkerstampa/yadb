package de.blogspot.volkersyadb.akkapersistence

import akka.pattern.ask
import de.blogspot.volkersyadb.akkapersistence.ItemActor.SaveSnapshot
import akka.persistence.{SaveSnapshotFailure, SaveSnapshotSuccess}
import akka.actor.Props

trait ItemApplicationTestExtensions { this: ItemApplication =>
  val itemServiceTestExtension = new ItemServiceTestExtensions(itemService)

  def shutdown()
}

// SNAPSHOT APPLICATION BEGIN
trait ItemApplicationWithSnapshot extends ItemApplicationTestExtensions
    with TestUtil { this: ItemApplication =>

  abstract override def itemActorProps: Props =
    Props(new ItemActor with RespondToSnapshotRequest)

  abstract override def shutdown() = {
    resultOf(itemActor ? SaveSnapshot) match {
      case _: SaveSnapshotSuccess =>
      case SaveSnapshotFailure(_, cause) =>
        sys.error(s"Saving snapshot failed with: $cause")
    }
    super.shutdown()
  }
}
// SNAPSHOT APPLICATION END
