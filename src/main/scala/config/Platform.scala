package org.nlogo.extensions.simpler.config

import java.awt.GraphicsEnvironment

object Platform {
  val isHeadless =
    GraphicsEnvironment.isHeadless ||
    "true".equals(System.getProperty("java.awt.headless")) ||
    "true".equals(System.getProperty("org.nlogo.preferHeadless"))

  val isWindows =
    System.getProperty("os.name").toLowerCase.startsWith("win")

}
