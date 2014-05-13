package de.blogspot.volkersyadb.akkapersistence

import scala.util.Try
import scala.concurrent.Future
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import akka.persistence.Persistent
import de.blogspot.volkersyadb.akkapersistence.ItemActor._

trait ItemService {
  def create(template: ItemTemplate): Future[Try[Item]]
  def update(item: Item): Future[Try[Item]]
  def delete(itemId: ItemId): Future[Try[Item]]
  def find(itemId: ItemId): Future[Option[Item]]
}

// ITEM SERVICE BEGIN
class ActorService(itemActor: ActorRef)(implicit timeout: Timeout) extends ItemService {
  def find(itemId: ItemId): Future[Option[Item]] =
    (itemActor ? GetItem(itemId)).mapTo[Option[Item]]

  def create(template: ItemTemplate): Future[Try[Item]] =
    (itemActor ? Persistent(CreateItem(template))).mapTo[Try[Item]]

  def delete(itemId: ItemId): Future[Try[Item]] =
    (itemActor ? Persistent(DeleteItem(itemId))).mapTo[Try[Item]]

  def update(item: Item): Future[Try[Item]] =
    (itemActor ? Persistent(UpdateItem(item))).mapTo[Try[Item]]
}
// ITEM SERVICE END

/*
  // ITEM SERVICE UPDATE BEGIN
  def update(item: Item): Future[Try[Item]] =
    (itemActor ? UpdateItem(item)).mapTo[Try[Item]]
  // ITEM SERVICE UPDATE END
 */