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

package org.digimead.digi.lib.jfx4swt

import com.sun.glass.ui.{ Cursor, Launchable, Pen, Pixels, Screen, Size, View, Window }
import com.sun.glass.ui.CommonDialogs.ExtensionFilter
import java.lang.ref.WeakReference
import java.nio.{ ByteBuffer, IntBuffer }
import java.util.TimerTask
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import org.digimead.digi.lib.jfx4swt.JFX.JFX2interface
import org.digimead.digi.lib.jfx4swt.jfx.{ FXAdapter, FXHost, FXHost7, JFaceCanvas, JFaceCanvas7 }
import scala.language.implicitConversions

/**
 * JFX4SWT adapter application.
 */
class JFXApplication7 extends JFXApplication {
  def createCursor(t: Int): Cursor = ???
  def createCursor(x: Int, y: Int, pixels: Pixels): Cursor = ???
  def createHost(adapter: FXAdapter): FXHost = new FXHost7(ref.WeakReference(adapter))
  def createJFaceCanvas(host: FXHost): JFaceCanvas = new JFaceCanvas7(ref.WeakReference(host))
  def createPixels(width: Int, height: Int, data: ByteBuffer): Pixels = ???
  def createPixels(width: Int, height: Int, data: IntBuffer): Pixels = ???
  def createPixels(width: Int, height: Int, data: IntBuffer, scale: Float): Pixels = ???
  def createRobot(): com.sun.glass.ui.Robot = ???
  def createTimer(runnable: Runnable) = new JFXApplication7.Timer(runnable)
  def createView(pen: Pen): View = new jfx.View(pen)
  def createWindow(owner: Window, screen: Screen, styleMask: Int): Window = new jfx.Window(owner, screen, styleMask)
  def createWindow(parent: Long): Window = ???
  def supportsTransparentWindows(): Boolean = false
  override protected def finishTerminating() {}
  override protected def shouldUpdateWindow(): Boolean = ???
  protected def _enterNestedEventLoop(): AnyRef = ???
  /**
   * Block the current thread and wait until the given  runnable finishes
   * running on the native event loop thread.
   */
  protected def _invokeAndWait(runnable: Runnable): Unit = {
    val completeLatch = new CountDownLatch(1)
    JFX.offer(new Runnable {
      def run {
        runnable.run()
        completeLatch.countDown()
      }
    })
    completeLatch.await()
  }
  /**
   * Schedule the given runnable to run on the native event loop thread
   * some time in the future, and return immediately.
   */
  protected def _invokeLater(runnable: Runnable) = JFX.offer(runnable)
  protected def _leaveNestedEventLoop(retValue: AnyRef) { ??? }
  protected def _postOnEventQueue(runnable: Runnable): Unit = JFX.offer(runnable)
  protected def runLoop(args: Array[String], launchable: Launchable) {
    JFX.offer(new Runnable {
      def run {
        if (getEventThread() == null)
          setEventThread(Thread.currentThread())
        if (getEventThread() != Thread.currentThread())
          throw new IllegalStateException(s"Unexpected event thread ${getEventThread()} vs current ${Thread.currentThread()}")
        launchable.finishLaunching(args)
      }
    })
  }
  protected def staticCommonDialogs_showFileChooser(a1: Window, a2: String, a3: String, a4: Int, a5: Boolean, a6: Array[ExtensionFilter]) = null
  protected def staticCommonDialogs_showFileChooser(a1: Window, a2: String, a3: String, a4: String, a5: Int, a6: Boolean, a7: Array[ExtensionFilter]) = null
  protected def staticCommonDialogs_showFolderChooser(a1: Window, a2: String, a3: String) = null
  protected def staticCursor_getBestSize(width: Int, height: Int): Size = ???
  protected def staticCursor_setVisible(visible: Boolean) = ???
  protected def staticPixels_getNativeFormat(): Int = ???
  /** Get deepest screen, but returns virtual. */
  protected def staticScreen_getDeepestScreen() = JFXApplication.virtualScreen
  /** Get main screen, but returns virtual. */
  protected def staticScreen_getMainScreen() = JFXApplication.virtualScreen
  /** Get screen for location, but returns virtual. */
  protected def staticScreen_getScreenForLocation(x: Int, f: Int) = JFXApplication.virtualScreen
  /** Get screen for pointer, but returns virtual. */
  protected def staticScreen_getScreenForPtr(nativePtr: Long) = JFXApplication.virtualScreen
  /** Get all available screens. Actually there is single virtual screen. */
  protected def staticScreen_getScreens() = {
    val result = new java.util.ArrayList[Screen]()
    result.add(JFXApplication.virtualScreen)
    result
  }
  protected def staticTimer_getMaxPeriod(): Int = 1000000
  protected def staticTimer_getMinPeriod(): Int = 0
  protected def staticView_getMultiClickMaxX(): Int = ???
  protected def staticView_getMultiClickMaxY(): Int = ???
  protected def staticView_getMultiClickTime(): Long = ???
}

object JFXApplication7 {
  /** Single application timer. */
  private lazy val timer: java.util.Timer = new java.util.Timer(true)

  class Timer(runnable: Runnable) extends com.sun.glass.ui.Timer(runnable) {
    private val task = new AtomicReference[java.util.TimerTask]()

    protected def _start(runnable: Runnable, period: Int) = {
      val newTask = new Task(runnable, new WeakReference(this))
      val previousTask = this.task.getAndSet(newTask)
      if (previousTask != null)
        previousTask.cancel()
      timer.schedule(newTask, 0, period)
      1 // need something non-zero to denote success.
    }
    protected def _start(runnable: Runnable) = throw new RuntimeException("vsync timer not supported");
    protected def _stop(timer: Long) {
      val previousTask = this.task.getAndSet(null)
      if (previousTask != null)
        previousTask.cancel()
    }
    class Task(runnable: Runnable, timer: WeakReference[Timer]) extends TimerTask {
      def run {
        runnable.run()
        val container = timer.get
        if (container != null)
          container.task.compareAndSet(this, null)
      }
    }
  }
}
