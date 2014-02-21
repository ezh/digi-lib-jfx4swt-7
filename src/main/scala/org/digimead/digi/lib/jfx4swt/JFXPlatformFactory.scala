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

package org.digimead.digi.lib.jfx4swt

import com.sun.glass.ui.{ Application, Menu, MenuBar, MenuItem, PlatformFactory, Window }

class JFXPlatformFactory extends PlatformFactory {
  def createApplication(): Application = new JFXApplication
  def createAccessibleRoot(node: AnyRef, window: Window) = throw new UnsupportedOperationException("Not supported forever.")
  def createAccessibleProvider(node: AnyRef) = throw new UnsupportedOperationException("Not supported forever.")
  def createMenuBarDelegate(menubar: MenuBar) = throw new UnsupportedOperationException("Not supported forever.")
  def createMenuDelegate(menu: Menu) = throw new UnsupportedOperationException("Not supported forever.")
  def createMenuItemDelegate(item: MenuItem) = throw new UnsupportedOperationException("Not supported forever.")
  def createClipboardDelegate() = throw new UnsupportedOperationException("Not supported forever.")
}
