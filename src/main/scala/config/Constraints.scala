package org.nlogo.extensions.simpler.config

import java.awt.{ Insets, GridBagConstraints }

object Constraints {
  def apply(
    gridx  : Integer = GridBagConstraints.RELATIVE,
    gridy  : Integer = GridBagConstraints.RELATIVE,
    gridw  : Integer = 1,
    gridh  : Integer = 1,
    weightx: Double  = 0.0,
    weighty: Double  = 0.0,
    anchor : Integer = GridBagConstraints.CENTER,
    fill   : Integer = GridBagConstraints.NONE,
    insets : Insets  = new Insets(0, 0, 0, 0),
    ipadx  : Integer = 0,
    ipady  : Integer = 0) =

    new GridBagConstraints(gridx, gridy, gridw, gridh, weightx, weighty, anchor, fill, insets, ipadx, ipady)
}
