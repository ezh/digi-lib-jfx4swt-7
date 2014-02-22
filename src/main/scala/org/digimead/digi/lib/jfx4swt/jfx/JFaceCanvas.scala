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

import com.sun.javafx.tk.{ TKStage, Toolkit }
import javafx.application.Platform
import javafx.scene.Scene
import org.digimead.digi.lib.Disposable
import org.digimead.digi.lib.jfx4swt.JFX
import org.digimead.digi.lib.log.api.Loggable
import scala.ref.WeakReference

class JFaceCanvas(host: WeakReference[FXHost]) extends javafx.stage.Window {
  @volatile protected var disposed = false

  /**
   * Close embedded frame.
   *
   * @param runnable run after close
   */
  def close(onDispose: JFaceCanvas ⇒ _) {
    if (disposed)
      throw new IllegalStateException(s"$this is disposed.")
    if (Platform.isFxApplicationThread())
      closeExec(onDispose)
    else
      JFX.exec { closeExec(onDispose) }
    disposed = true
  }
  /**
   * Open embedded frame.
   *
   * @param scene scene to render
   * @param runnable run after open
   */
  def open(scene: Scene, onReady: JFaceCanvas ⇒ _) {
    if (disposed)
      throw new IllegalStateException(s"$this is disposed.")
    if (Platform.isFxApplicationThread())
      openExec(scene, onReady)
    else
      JFX.exec { openExec(scene, onReady) }
  }

  /** Execute close sequence within Java FX thread. */
  protected def closeExec(onDispose: JFaceCanvas ⇒ _) = {
    @deprecated("", "") def hide_warning_for_impl_peer_get = impl_peer
    @deprecated("", "") def hide_warning_for_impl_peer_set(arg: TKStage) = impl_peer = arg
    val embeddedStage = hide_warning_for_impl_peer_get
    hide()
    if (embeddedStage != null) {
      embeddedStage.setVisible(false)
      embeddedStage.setScene(null)
      embeddedStage.close()
    }
    host.get.foreach(_.dispose())
    hide_warning_for_impl_peer_set(null)
    JFX.execAsync {
      try onDispose(this)
      catch { case e: Throwable ⇒ JFaceCanvas.log.error("onDispose callback failed." + e.getMessage(), e) }
    }
  }
  /** Execute open sequence within Java FX thread. */
  protected def openExec(scene: Scene, onReady: JFaceCanvas ⇒ _) = {
    @deprecated("", "") def hide_warning_for_impl_peer_set(arg: TKStage) = impl_peer = arg
    if (getScene() == null) {
      setScene(scene)
      host.get.foreach(host ⇒ hide_warning_for_impl_peer_set(Toolkit.getToolkit.createTKEmbeddedStage(host)))
      show()
    } else {
      setScene(scene)
    }
    JFX.execAsync {
      // Some times embedded content freeze at the beginning.
      host.get.foreach(_.embeddedScene.foreach { _.entireSceneNeedsRepaint() })
      try onReady(this)
      catch { case e: Throwable ⇒ JFaceCanvas.log.error("onReady callback failed." + e.getMessage(), e) }
    }
  }
}

object JFaceCanvas extends Loggable
