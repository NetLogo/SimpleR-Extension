package org.nlogo.extensions.simpler.config

import java.io.Closeable

object Using {
  def apply[A <: Closeable, B](resource: A)(fn: A => B): B =
    apply(resource, (x: A) => x.close())(fn)

  def apply[A, B](resource: A, cleanup: A => Unit)(fn: A => B): B =
    try fn(resource) finally if (resource != null) cleanup(resource)
}
