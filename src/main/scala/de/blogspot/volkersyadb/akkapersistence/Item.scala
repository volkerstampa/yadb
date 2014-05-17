package de.blogspot.volkersyadb.akkapersistence

trait CommonItem extends NotNull {
  def description: String
  def rank: Option[Int]
}

// ITEM BEGIN
class ItemId(val id: Long) extends AnyVal with Serializable {
  override def toString = id.toString
}
object ItemId {
  def apply(id: Long): ItemId = new ItemId(id)
}
case class ItemTemplate(description: String, rank: Option[Int] = None) extends CommonItem
case class Item(id: ItemId, description: String, rank: Option[Int] = None) extends CommonItem {
  def asTemplate: ItemTemplate = ItemTemplate(description)
  def withTemplate(template: ItemTemplate): Item =
    copy(description = template.description)
}
// ITEM END
