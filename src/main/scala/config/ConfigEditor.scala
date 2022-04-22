package org.nlogo.extensions.simpler.config

import java.awt.{ BorderLayout, FileDialog, GridBagLayout, GridBagConstraints => GBC }
import java.io.File
import javax.swing.{ BorderFactory, JButton, JDialog, JFrame, JLabel, JPanel, JTextField }

import org.nlogo.core.I18N
import org.nlogo.swing.{ ButtonPanel, RichAction, RichJButton, Utils }

class ConfigEditor(owner: JFrame, longName: String, extLangBin: String, config: Config) extends JDialog(owner, longName) {
  private val runtimePathTextField = new JTextField(config.runtimePath.getOrElse(""), 20)

  {
    getContentPane.setLayout(new BorderLayout)
    val mainPanel = new JPanel
    mainPanel.setLayout(new BorderLayout)
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5))
    getContentPane.add(mainPanel, BorderLayout.CENTER)

    val editPanel = new JPanel
    editPanel.setLayout(new GridBagLayout)

    editPanel.add(new JLabel(
      s"Enter the path to your $extLangBin executable. If blank, the $longName will attempt to find an appropriate version of $extLangBin to run on the system's PATH."
    ), Constraints(gridx = 0, gridw = 3))

    val pathLabel = s"$extLangBin Path"
    editPanel.add(new JLabel(pathLabel), Constraints(gridx = 0))
    editPanel.add(runtimePathTextField, Constraints(gridx = 1, weightx = 1.0, fill = GBC.HORIZONTAL))
    editPanel.add(RichJButton("Browse...") {
      val userSelected = askForPath(pathLabel, runtimePathTextField.getText)
      userSelected.foreach(runtimePathTextField.setText)
    }, Constraints(gridx = 2))

    val okButton = RichJButton(I18N.gui.get("common.buttons.ok")) {
      save()
      dispose()
    }
    val cancelAction = RichAction(I18N.gui.get("common.buttons.cancel"))(_ => dispose())
    val buttonPanel = ButtonPanel(
      okButton,
      new JButton(cancelAction)
    )
    getRootPane.setDefaultButton(okButton)
    Utils.addEscKeyAction(this, cancelAction)

    mainPanel.add(editPanel, BorderLayout.CENTER)
    mainPanel.add(buttonPanel, BorderLayout.SOUTH)
    pack()
  }

  def askForPath(name: String, current: String): Option[String] = {
    val dialog = new FileDialog(this, s"Configure $name", FileDialog.LOAD)
    dialog.setDirectory(new File(current).getParent)
    dialog.setFile(new File(current).getName)
    dialog.setVisible(true)
    Option(dialog.getDirectory)
  }

  def save(): Unit = {
    config.runtimePath = runtimePathTextField.getText
    config.save
  }
}
