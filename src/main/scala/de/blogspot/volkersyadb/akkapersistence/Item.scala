package de.blogspot.volkersyadb.akkapersistence

trait CommonItem extends NotNull {
  def description: String
}

// ITEM BEGIN
class ItemId(val id: Long) extends AnyVal with Serializable {
  override def toString = id.toString
}
object ItemId {
  def apply(id: Long): ItemId = new ItemId(id)
}
case class ItemTemplate(description: String) extends CommonItem
case class Item(id: ItemId, description: String) extends CommonItem {
  def asTemplate: ItemTemplate = ItemTemplate(description)
  def withTemplate(template: ItemTemplate): Item =
    copy(description = template.description)
}
// ITEM END
