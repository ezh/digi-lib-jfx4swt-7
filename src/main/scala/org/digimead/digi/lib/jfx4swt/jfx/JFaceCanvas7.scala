/**
 * JFX4SWT-7 - Java 7 JavaFX library adapter for SWT framework.
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

import com.sun.javafx.tk.Toolkit
import javafx.scene.Scene
import org.digimead.digi.lib.Disposable
import org.digimead.digi.lib.jfx4swt.JFX
import scala.ref.WeakReference

class JFaceCanvas7(host: WeakReference[FXHost]) extends JFaceCanvas(host) {
  protected val lock = new Object
  /** Execute close sequence within Java FX thread. */
  @SuppressWarnings(Array("deprecation"))
  protected def closeExec(onDispose: JFaceCanvas ⇒ _) = JFX.execAsync {
    lock.synchronized {
      val embeddedStage = impl_peer
      val embeddedScene = Option(getScene()).map(_.impl_getPeer()) getOrElse null
      hide()
      if (embeddedStage != null) {
        embeddedStage.setVisible(false)
        embeddedStage.setScene(null)
        embeddedStage.close()
      }
      host.get.foreach(_.asInstanceOf[FXHost7].dispose())
      impl_peer = null
      JFX.execAsync {
        // There are circular references that ignored by GC
        if (embeddedStage != null)
          Disposable.clean(embeddedStage)
        if (embeddedScene != null)
          Disposable.clean(embeddedScene)
        try onDispose(this)
        catch { case e: Throwable ⇒ JFaceCanvas.log.error("onDispose callback failed." + e.getMessage(), e) }
      }
    }
  }
  /** Execute open sequence within Java FX thread. */
  protected def openExec(scene: Scene, onReady: JFaceCanvas ⇒ _) = JFX.execAsync {
    lock.synchronized {
      host.get.foreach(_.asInstanceOf[FXHost7].setScene(scene))
      if (getScene() == null) {
        setScene(scene)
        host.get.foreach(host ⇒ { impl_peer = Toolkit.getToolkit.createTKEmbeddedStage(host) })
      } else {
        setScene(scene)
      }
      show()
      JFX.execAsync {
        // Some times embedded content freeze at the beginning.
        lock.synchronized { host.get.foreach(_.asInstanceOf[FXHost7].embeddedScene.foreach { _.entireSceneNeedsRepaint() }) }
        try onReady(this)
        catch { case e: Throwable ⇒ JFaceCanvas.log.error("onReady callback failed." + e.getMessage(), e) }
      }
    }
  }
}
