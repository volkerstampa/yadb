package de.blogspot.volkersyadb.akkapersistence

import java.io.File
import com.google.common.io.Files
import scala.util.control.Exception._
import org.iq80.leveldb.util.FileUtils
import org.scalatest.{Outcome, fixture}

trait TempDirFixture extends WithTempDir { this: fixture.Suite =>

  type FixtureParam = File

  def withFixture(test: OneArgTest): Outcome =
    withTempDir(tmpDir => withFixture(test.toNoArgTest(tmpDir)))
}

trait WithTempDir {
  def withTempDir[A](block: File => A): A = {
    val tmpDir = Files.createTempDir()
    ultimately(FileUtils.deleteRecursively(tmpDir))(block(tmpDir))
  }
}
