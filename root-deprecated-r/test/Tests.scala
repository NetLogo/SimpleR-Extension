package org.nlogo.extensions.simpler

import java.io.File
import org.nlogo.headless.TestLanguage

object Tests {
  val testFileNames = Seq("tests-r.txt")
  val testFiles     = testFileNames.map( (f) => (new File(f)).getCanonicalFile )
}

class Tests extends TestLanguage(Tests.testFiles) {
  System.setProperty("org.nlogo.preferHeadless", "true")
  System.setProperty("java.awt.headless", "true")
}
