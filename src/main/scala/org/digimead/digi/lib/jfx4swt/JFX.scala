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

import com.sun.glass.ui.PlatformFactory
import com.sun.javafx.application.PlatformImpl
import java.util.concurrent.{ ConcurrentLinkedQueue, CountDownLatch }
import org.digimead.digi.lib.api.DependencyInjection
import org.digimead.digi.lib.log.api.Loggable
import org.eclipse.swt.graphics.PaletteData
import scala.annotation.tailrec
import scala.language.implicitConversions

/**
 * Lightweight JavaFX launcher.
 */
class JFX extends Loggable {
  /** Default palette for ImageData. */
  val paletteData = new PaletteData(0xFF00, 0xFF0000, 0xFF000000)
  /** Event queue. */
  protected[this] val bufferedQueue = new ConcurrentLinkedQueue[Runnable]
  /** Event queue thread. */
  protected[this] val thread = new JFX.EventThread(bufferedQueue)

  /** Start event thread. */
  def start(runnable: Runnable = new Runnable { def run {} }, priority: Int = Thread.MAX_PRIORITY) {
    val startupLatch = new CountDownLatch(1)
    if (System.getProperty("quantum.multithreaded") == null)
      if (JFX.multithreaded)
        System.setProperty("quantum.multithreaded", "true")
      else
        System.setProperty("quantum.multithreaded", "false")
    val factoryInstanceField = classOf[PlatformFactory].getDeclaredField("instance")
    if (!factoryInstanceField.isAccessible())
      factoryInstanceField.setAccessible(true)
    factoryInstanceField.set(null, new JFXPlatformFactory)
    if (PlatformFactory.getPlatformFactory().getClass() != classOf[JFXPlatformFactory])
      throw new IllegalStateException("Unexpected JavaFX platform factory: " + PlatformFactory.getPlatformFactory())
    thread.setDaemon(true)
    thread.setPriority(priority)
    thread.start()
    try {
      PlatformImpl.startup(new Runnable {
        def run {
          runnable.run()
          startupLatch.countDown()
          log.debug("JFX4SWT started.")
        }
      })
      startupLatch.await()
    } catch { case e: Throwable ⇒ log.error(e.getMessage, e) }
  }
  /** Add event to buffered queue. */
  def offer(event: Runnable) = bufferedQueue.synchronized {
    bufferedQueue.offer(event)
    bufferedQueue.notifyAll
  }
  /** Stop event thread. */
  def stop(runnable: Runnable = new Runnable { def run {} }) {
    val stopLatch = new CountDownLatch(1)
    thread.stop(new Runnable {
      def run {
        runnable.run()
        stopLatch.countDown()
        log.debug("JFX4SWT stopped.")
      }
    })
    stopLatch.await()
  }
}

object JFX extends jfx.Thread with Loggable {
  implicit def JFX2interface(g: JFX.type): JFX = DI.implementation

  /** JFX debug flag. */
  def debug = DI.debug
  /** Multithread flag for Quantum backend. */
  def multithreaded = DI.multithreaded
  /** JFX short runnable timeout in milliseconds. */
  def timeout = DI.timeout

  /** Attribute that affects debugging event loop runnables. */
  trait EventLoopRunnableDuration
  /** Short running runnable. */
  case object ShortRunnable extends EventLoopRunnableDuration
  /** Long running runnable (like dialog, that waiting user input). */
  case object LongRunnable extends EventLoopRunnableDuration
  /** Event loop implementation. */
  class EventThread(bufferedQueue: ConcurrentLinkedQueue[Runnable]) extends Thread(s"JavaFX Application Thread") {
    protected[this] var running = true

    /** Event loop. */
    @tailrec
    final override def run() = {
      if (!bufferedQueue.isEmpty) {
        try {
          var event = bufferedQueue.poll()
          while (event != null) {
            event.run
            event = bufferedQueue.poll()
          }
        } catch {
          case e: Throwable ⇒ log.error(e.getMessage(), e)
        }
      } else
        bufferedQueue.synchronized { bufferedQueue.wait }
      if (running)
        run
    }
    /** Stop event loop. */
    def stop(event: Runnable) = bufferedQueue.synchronized {
      bufferedQueue.offer(new Runnable {
        def run {
          EventThread.this.running = false
          event.run()
        }
      })
      bufferedQueue.notifyAll
    }
  }
  /**
   * Dependency injection routines.
   */
  private object DI extends DependencyInjection.PersistentInjectable {
    /** JFX debug flag. */
    lazy val debug = injectOptional[Boolean]("JFX.Debug") getOrElse false
    /** Multithread flag for Quantum backend. */
    lazy val multithreaded = injectOptional[Boolean]("JFX.Multithreaded") getOrElse true
    /** JFX short runnable timeout in milliseconds. */
    lazy val timeout = injectOptional[Int]("JFX.ShortRunnableTimeout") getOrElse 1000
    /** JFX implementation. */
    lazy val implementation = injectOptional[JFX] getOrElse new JFX
  }
}
