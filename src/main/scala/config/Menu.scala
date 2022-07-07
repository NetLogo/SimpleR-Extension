package org.nlogo.extensions.simpler.config

import javax.swing.JMenu

import org.nlogo.app.App

import org.nlogo.languagelibrary.ShellWindow

object Menu {

  def create(longName: String, extLangBin: String, config: Config): Option[Menu] = {
    if (Platform.isHeadless) {
      None
    } else {
      val menuBar   = App.app.frame.getJMenuBar
      // I'm struggly to think how this would need to be called, as the extension should
      // always be unloaded and removed from the menu before being re-created for a new
      // model or whatever, but I guess it doesn't hurt to leave it in?
      // -Jeremy B April 2022
      val maybeMenu = menuBar.getComponents.collectFirst {
        case mi: Menu if mi.getText == longName => mi
      }
      Option(maybeMenu.getOrElse({
        val shellWindow = new ShellWindow()
        val menu        = new Menu(shellWindow, longName, extLangBin, config)
        menuBar.add(menu)
        menu
      }))
    }
  }

}

class Menu(private val shellWindow: ShellWindow, longName: String, extLangBin: String, config: Config) extends JMenu(longName) {
  def setup(evalStringified: (String) => String) = {
    if (!Platform.isHeadless) {
      shellWindow.setEvalStringified(Some(evalStringified))
    }
  }

  def unload() = {
    if (!Platform.isHeadless) {
      shellWindow.setVisible(false)
      App.app.frame.getJMenuBar.remove(this)
    }
  }

  add("Configure").addActionListener { _ =>
    new ConfigEditor(App.app.frame, longName, extLangBin, config).setVisible(true)
  }

  add("Pop-out Interpreter").addActionListener { _ =>
    shellWindow.setVisible(!shellWindow.isVisible)
  }

}
