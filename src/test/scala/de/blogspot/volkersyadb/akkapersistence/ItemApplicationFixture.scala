package de.blogspot.volkersyadb.akkapersistence

import org.scalatest.{TestData, Suite, Outcome, fixture}
import java.io.File
import scala.util.control.Exception.ultimately
import scala.collection.JavaConverters._
import com.typesafe.config.{Config, ConfigFactory}
import org.iq80.leveldb.util.FileUtils
import com.google.common.io.Files
import com.google.common.base.Charsets

trait ItemApplicationFixture extends WithItemApplication with WithTempDir {
    this: fixture.Suite =>

  type FixtureParam = TestApplication

  def withFixture(test: OneArgTest): Outcome = {

    withTempDir(withApplication(_)(
        application => withFixture(test.toNoArgTest(application))))
  }
}

trait WithItemApplication {
  final val JournalDirConfig = "akka.persistence.journal.leveldb.dir"
  final val NativeLevelDbConfig = "akka.persistence.journal.leveldb.native"
  final val SnapshotDirConfig = "akka.persistence.snapshot-store.local.dir"

  type TestApplication = ItemApplication with ItemApplicationTestExtensions
  // WITH ITEM APPLICATION BEGIN
  def withApplication[A](persistDir: File)(block: TestApplication => A): A = {
    val tmpDirPersistenceConfig = ConfigFactory.parseMap(
      Map(JournalDirConfig -> new File(persistDir, "journal").getPath,
        NativeLevelDbConfig -> false.toString,
        SnapshotDirConfig -> new File(persistDir, "snapshots").getPath)
        .asJava)
    val application = newItemApplication(tmpDirPersistenceConfig)
    ultimately(application.shutdown())(block(application))
  }

  def newItemApplication(config: Config) =
    new ItemApplication(config) with ItemApplicationTestExtensions
  // WITH ITEM APPLICATION END

  def startApplication[A](persistDir: File)(block: TestApplication => A): A =
    withApplication(persistDir)(block)

  def restartApplication[A](persistDir: File)(block: TestApplication => A): A =
    withApplication(persistDir)(block)
}

// WITH SNAPSHOT APPLICATION BEGIN
trait WithItemApplicationWithSnapshot extends WithItemApplication {
  override def newItemApplication(config: Config) =
    new ItemApplication(config) with ItemApplicationWithSnapshot
}
// WITH SNAPSHOT APPLICATION END

trait WithItemApplicationWithSaveJournal[T <: Suite] extends WithItemApplication
    with MigrationUtil[T] { this: T =>

  // WITH SAVE JOURNAL APPLICATION BEGIN
  override def startApplication[A](persistDir: File)(block: TestApplication => A): A = {
    val result = super.startApplication(persistDir)(block)
    saveData(persistDir, result)
    result
  }

  def saveData[A](persistDir: File, result: A): Unit = {
    val destinationDir = migrationDataDir
    if(destinationDir.isDirectory)
      FileUtils.deleteDirectoryContents(destinationDir)
    FileUtils.copyDirectoryContents(persistDir, destinationDir)
    Files.write(result.toString, new File(destinationDir, "expected.txt"),
      Charsets.UTF_8)
  }
  // WITH SAVE JOURNAL APPLICATION END
}

trait WithItemApplicationWithMigration[T <: Suite] extends WithItemApplication
    with MigrationUtil[T] { this: T =>

  // WITH MIGRATION APPLICATION BEGIN
  protected def expectedValueFor(test: TestData): Any

  override def startApplication[A](persistDir: File)(block: (TestApplication) => A): A =
    expectedValueFor(currentTest).asInstanceOf[A]

  override def restartApplication[A](persistDir: File)(block: (TestApplication) => A): A =
    super.restartApplication(migrationDataDir)(block)
  // WITH MIGRATION APPLICATION END
}
