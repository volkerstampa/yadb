package de.blogspot.volkersyadb.akkapersistence

class ItemServiceTestExtensions(service: ItemService) extends ItemFactory
    with TestUtil {

  // CREATE NEW ITEM BEGIN
  def createNewItem(template: ItemTemplate = newItemTemplate()): Item =
    successOf(service.create(template))
  // CREATE NEW ITEM END

  def updateItem(item: Item, template: ItemTemplate = newItemTemplate()): Item =
    successOf(service.update(item.withTemplate(template)))

  def deleteItem(itemId: ItemId): Item =
    successOf(service.delete(itemId))

  def findItem(itemId: ItemId): Option[Item] =
    resultOf(service.find(itemId))
}