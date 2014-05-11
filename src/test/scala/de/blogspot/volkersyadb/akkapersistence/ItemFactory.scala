package de.blogspot.volkersyadb.akkapersistence

import java.util.concurrent.atomic.AtomicLong

object ItemFactory {
  val counter = new AtomicLong(0)
}

trait ItemFactory {
  def uniqueLong: Long = ItemFactory.counter.incrementAndGet()
  def uniqueString(suffix: String): String = s"$uniqueLong - $suffix"

  def newDescription: String = uniqueString("description")
  def newItemTemplate(description: String = newDescription)
      : ItemTemplate =
    ItemTemplate(description)

  def newId: ItemId = ItemId(uniqueLong)

  def newItem(id: ItemId = newId,
              description: String = newDescription): Item =
    Item(id, description)
}
