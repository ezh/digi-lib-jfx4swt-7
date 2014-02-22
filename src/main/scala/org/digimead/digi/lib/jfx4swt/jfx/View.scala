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

import com.sun.glass.ui.{ Cursor, Pixels, Screen, View ⇒ JFXView, Window ⇒ JFXWindow }
import com.sun.glass.ui.Pen

class View(pen: Pen) extends JFXView(pen) {
  /**
   * Prepares to painting by locking native surface.
   *
   * Called on the render thread.
   */
  protected def _begin(pointer: Long, a: Boolean): Boolean = true
  protected def _close(pointer: Long) = true
  protected def _create(caps: java.util.Map[_, _]): Long = {
    View.instanceCounter += 1;
    View.instanceCounter
  }
  protected def _enableInputMethodEvents(pointer: Long, enable: Boolean): Unit = ???
  /**
   * Ends painting by unlocking native surface and flushing
   * flushes surface (if flush == true) or discard it (flush == false)
   *
   * Called on the render thread
   */
  protected def _end(pointer: Long, a: Boolean, b: Boolean) {}
  protected def _enterFullscreen(pointer: Long, animate: Boolean, keepRatio: Boolean, hideCursor: Boolean): Boolean = true
  protected def _exitFullscreen(pointer: Long, animate: Boolean) {}
  protected def _getNativeView(pointer: Long) = pointer
  protected def _getX(pointer: Long): Int = 0
  protected def _getY(pointer: Long): Int = 0
  protected def _repaint(pointer: Long) {}
  protected def _setParent(pointer: Long, parentPtr: Long) {}
  protected def _uploadPixels(pointer: Long, pixels: Pixels) {}
}

object View {
  // There are only 2^64 - 1 views. Please restart this software before the limit will be reached. ;-)
  protected var instanceCounter = Long.MinValue
}
