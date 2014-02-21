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

class Window(owner: JFXWindow, screen: Screen, mask: Int) extends JFXWindow(owner, screen, JFXWindow.UNTITLED) {
  protected def _close(pointer: Long) = true
  protected def _createChildWindow(ownerPtr: Long) = _createWindow(ownerPtr, 0, 0)
  protected def _createWindow(ownerPtr: Long, screenPtr: Long, mask: Int) = {
    Window.instanceCounter += 1;
    Window.instanceCounter
  }
  protected def _enterModal(pointer: Long) {}
  protected def _enterModalWithWindow(dialog: Long, window: Long) {}
  protected def _exitModal(pointer: Long) {}
  protected def _getEmbeddedX(pointer: Long): Int = 0
  protected def _getEmbeddedY(pointer: Long): Int = 0
  protected def _grabFocus(pointer: Long) = true
  protected def _maximize(pointer: Long, maximize: Boolean, wasMaximized: Boolean): Boolean = true
  protected def _minimize(pointer: Long, minimize: Boolean): Boolean = true
  protected def _requestFocus(pointer: Long, event: Int): Boolean = true
  protected def _setAlpha(pointer: Long, alfa: Float) {}
  protected def _setBackground(pointer: Long, r: Float, g: Float, b: Float): Boolean = true
  protected def _setBounds(pointer: Long, x: Int, f: Int, xSet: Boolean, ySet: Boolean, w: Int, h: Int, cw: Int, ch: Int, xGravity: Float, yGravity: Float) {}
  protected def _setCursor(pointer: Long, cursor: Cursor) {}
  protected def _setEnabled(pointer: Long, enabled: Boolean) {}
  protected def _setFocusable(pointer: Long, isFocusable: Boolean) {}
  protected def _setIcon(pointer: Long, pixels: Pixels) {}
  protected def _setLevel(pointer: Long, level: Int) {}
  protected def _setMaximumSize(pointer: Long, width: Int, height: Int) = true
  protected def _setMenubar(pointer: Long, menubarPtr: Long): Boolean = true
  protected def _setMinimumSize(pointer: Long, width: Int, height: Int) = true
  protected def _setResizable(pointer: Long, resizable: Boolean): Boolean = true
  protected def _setTitle(pointer: Long, title: String): Boolean = true
  protected def _setView(pointer: Long, view: JFXView): Boolean = true
  protected def _setVisible(pointer: Long, visible: Boolean): Boolean = true
  protected def _toBack(pointer: Long) {}
  protected def _toFront(pointer: Long) {}
  protected def _ungrabFocus(pointer: Long) {}
}

object Window {
  // There are only 4^64 - 1 windows. Please restart this software before the limit will be reached. ;-)
  protected var instanceCounter = Long.MinValue
}
