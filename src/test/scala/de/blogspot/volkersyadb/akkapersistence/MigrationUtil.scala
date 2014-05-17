package de.blogspot.volkersyadb.akkapersistence

import org.scalatest.{TestData, Suite}
import scala.reflect.ClassTag
import java.io.File

trait MigrationUtil[T <: Suite] extends Suite { this: T =>

  protected def runId: String
  protected def tag: ClassTag[T]

  protected var currentTest: TestData = _

  protected val SavedJournalsDir = new File("src/test/saved-journals")
  protected def suiteDirName: String = tag.runtimeClass.getName
  protected val suiteDir =
    new File(new File(SavedJournalsDir, runId), suiteDirName)
  protected def migrationDataDir: File = new File(suiteDir, currentTest.name)

  override protected def withFixture(test: NoArgTest) = {
    currentTest = test
    super.withFixture(test)
  }
}
