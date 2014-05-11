package de.blogspot.volkersyadb.akkapersistence

import scala.util.{Failure, Success, Try}
import akka.persistence._
import akka.actor.{Props, ActorRef}


// ITEM ACTOR BEGIN
class ItemActor extends Processor {

  import ItemActor._

  private var itemById: Map[ItemId, Item] = Map.empty
  private var idCounter: Long = 0

  def receive = {
    case Persistent(CreateItem(template: ItemTemplate), _) =>
      idCounter += 1
      sender ! addItem(Item(ItemId(idCounter), template.description))

    case Persistent(UpdateItem(item: Item), _) =>
      sender ! (if(itemById.isDefinedAt(item.id))
        addItem(item)
      else
        Failure(new NonExistingItemCannotBeUpdatedException(item)))

    case Persistent(DeleteItem(itemId: ItemId), _) =>
      val deletedItem = itemById.get(itemId)
      deletedItem.foreach(itemById -= _.id)
      sender ! deletedItem.fold[Try[Item]](
        Failure(new NonExistingItemCannotBeDeletedException(itemId)))(
        Success.apply)

    case GetItem(itemId: ItemId) =>
      sender ! itemById.get(itemId)

    // SNAPSHOT BEGIN
    case SaveSnapshot => saveSnapshot(ItemActorSnapshot(itemById, idCounter))
    case SaveSnapshotSuccess(metadata) =>
    case SaveSnapshotFailure(metadata, cause) =>

    case SnapshotOffer(_, ItemActorSnapshot(itemMap, lastId)) =>
      this.itemById = itemMap
      this.idCounter = lastId
    // SNAPSHOT END
  }

  private def addItem(item: Item): Try[Item] = {
    itemById += (item.id -> item)
    Success(item)
  }
}
// ITEM ACTOR END

object ItemActor {
  def props(): Props = Props(new ItemActor)

  sealed trait Command extends Serializable with NotNull
  case class CreateItem(template: ItemTemplate) extends Command
  case class UpdateItem(item: Item) extends Command
  case class DeleteItem(item: ItemId) extends Command
  case class GetItem(item: ItemId) extends Command

  case object SaveSnapshot

  case class ItemActorSnapshot(itemById: Map[ItemId, Item], idCounter: Long)

  class ItemDoesNotExistException(val itemId: ItemId) extends Exception {
    override def getMessage: String = s"Item with id '$itemId' does not exist"
  }
  class NonExistingItemCannotBeUpdatedException(val item: Item)
      extends ItemDoesNotExistException(item.id) {
    override def getMessage: String =
      s"Item '$item' cannot be updated as ${super.getMessage}"
  }
  class NonExistingItemCannotBeDeletedException(itemId: ItemId)
      extends ItemDoesNotExistException(itemId) {
    override def getMessage: String =
      s"${super.getMessage} and cannot be deleted"
  }
}
