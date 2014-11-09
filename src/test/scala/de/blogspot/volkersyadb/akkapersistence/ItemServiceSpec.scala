package de.blogspot.volkersyadb.akkapersistence

import org.scalatest.fixture
import org.scalatest.junit.JUnitRunner

class ItemServiceSpec extends fixture.WordSpec with TestCommons
    with ItemApplicationFixture {

  s"${n[ItemService]}.createItem" when {
    s"invoked with a ${n[ItemTemplate]}" must {
      s"return the created ${n[Item]}" in { application =>
        val template = newItemTemplate()

        val item = successOf(application.itemService.create(template))

        item.asTemplate should be (template)
      }
    }
  }

  s"${n[ItemService]}.updateItem" when {
    s"invoked with an existing ${n[Item]}" must {
      s"return the updated ${n[Item]}" in { application =>
        val initial = application.itemServiceTestExtension.createNewItem()

        val modified = initial.copy(description = newDescription)
        val updated = successOf(application.itemService.update(modified))

        updated should be (modified)
      }
    }
  }

  s"${n[ItemService]}.find" when {
    s"invoked with an existing ${n[ItemId]}" must {
      s"return the ${n[Item]}" in { application =>
        val expected = application.itemServiceTestExtension.createNewItem()

        val actual = valueOf(application.itemService.find(expected.id))

        actual should be (expected)
      }
    }
  }

  s"${n[ItemService]}.delete" when {
    s"invoked with an existing ${n[ItemId]}" must {
      s"return the ${n[Item]}" in { application =>
        val serviceExt = application.itemServiceTestExtension
        val expected = serviceExt.createNewItem()

        val actual = successOf(application.itemService.delete(expected.id))

        actual should be (expected)
        serviceExt.findItem(expected.id) should be (None)
      }
    }
  }
}
