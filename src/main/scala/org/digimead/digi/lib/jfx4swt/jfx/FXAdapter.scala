/**
 * JFX4SWT - JavaFX library adapter for SWT framework.
 *
 * Copyright (c) 2014 Alexey Aksenov ezh@ezh.msk.ru
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.digimead.digi.lib.jfx4swt.jfx

import java.util.concurrent.atomic.AtomicReference

/**
 * FXAdapter that binds JavaFX to container.
 */
trait FXAdapter {
  /** Preallocated Array[Byte] w*h for container ImageData. */
  val frameOne = new AtomicReference[Array[Byte]]()
  /** Preallocated Array[Byte] w*h for container ImageData. */
  val frameTwo = new AtomicReference[Array[Byte]]()
  /** Pointer with free frame (frameOne/frameTwo). */
  val frameEmpty = new AtomicReference[Array[Byte]]()
  /** Pointer with rendered frame (frameOne/frameTwo). */
  val frameFull = new AtomicReference[Array[Byte]]()

  def dispose() {
    frameOne.set(null)
    frameTwo.set(null)
    frameEmpty.set(null)
    frameFull.set(null)
  }
  /**
   * Called by FXHost from JavaFX event thread when frameFull are ready.
   */
  def redraw()
  /**
   * Forces the receiver to have the <em>keyboard focus</em>, causing
   * all keyboard events to be delivered to it.
   * Called from JavaFX event thread.
   *
   * @return <code>true</code> if the control got focus, and <code>false</code> if it was unable to.
   */
  def requestFocus(): Boolean
  /**
   * Enables the receiver if the argument is <code>true</code>,
   * and disables it otherwise. A disabled control is typically
   * not selectable from the user interface and draws with an
   * inactive or "grayed" look.
   * Called from JavaFX event thread.
   *
   * @param enabled the new enabled state
   */
  def setEnabled(enabled: Boolean)
  /**
   * Set container preferred size based on scene content.
   * Called from JavaFX event thread.
   */
  def setPreferredSize(x: Int, y: Int)
  /**
   * Called by embedded FX scene from JavaFX event thread to traverse focus to a component
   * which is next/previous to this container in an emedding app.
   */
  def traverseFocusOut(forward: Boolean): Boolean
}
