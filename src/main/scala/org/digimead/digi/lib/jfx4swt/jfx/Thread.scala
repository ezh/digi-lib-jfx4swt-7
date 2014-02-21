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

import java.util.concurrent.{ Exchanger, ExecutionException, TimeUnit }
import javafx.application.Platform
import org.digimead.digi.lib.jfx4swt.JFX

trait Thread {
  this: org.digimead.digi.lib.jfx4swt.JFX.type ⇒

  /** Assert the current thread against JavaFX application one. */
  def assertEventThread(EventLoop: Boolean = true) = if (EventLoop) {
    if (!Platform.isFxApplicationThread()) {
      val throwable = new IllegalAccessException("Only the original thread that created the the JavaFX thread can touch its widgets.")
      // sometimes we throw exception in threads that haven't catch block, notify anyway
      log.error("Only the original thread that created the JavaFX thread can touch its widgets.", throwable)
      throw throwable
    }
  } else {
    if (Platform.isFxApplicationThread()) {
      val throwable = new IllegalAccessException(s"Current thread ${Thread.currentThread()} is the JavaFX thread blocker.")
      // sometimes we throw exception in threads that haven't catch block, notify anyway
      log.error(s"Current thread ${Thread.currentThread()} is the JavaFX thread blocker.", throwable)
      throw throwable
    }
  }
  /** Execute runnable in the JavaFX application thread. */
  def exec[T](f: ⇒ T)(implicit duration: JFX.EventLoopRunnableDuration = JFX.ShortRunnable): Unit =
    if (Platform.isFxApplicationThread()) {
      try if (JFX.debug) {
        val t = new Throwable(s"Entry point from ${java.lang.Thread.currentThread.getName()}.")
        val ts = System.currentTimeMillis()
        f
        val duration = System.currentTimeMillis() - ts
        if (duration > 500)
          log.error(s"Too heavy operation: ${duration}ms.", t)
      } else f
      catch { case e: Throwable ⇒ log.error("Event thread exception: " + e, e) }
    } else execAsync({ f })
  /** Asynchronously execute runnable in the JavaFX application thread. */
  def execAsync[T](f: ⇒ T)(implicit duration: JFX.EventLoopRunnableDuration = JFX.ShortRunnable): Unit = {
    if (duration == JFX.ShortRunnable && JFX.debug) {
      val t = new Throwable(s"Entry point from ${java.lang.Thread.currentThread.getName()}.")
      Platform.runLater(new Runnable {
        def run = try {
          val ts = System.currentTimeMillis()
          f
          val duration = System.currentTimeMillis() - ts
          if (duration > 500)
            log.error(s"Too heavy operation: ${duration}ms.", t)
        } catch { case e: Throwable ⇒ log.error("Event thread exception: " + e, e) }
      })
    } else {
      Platform.runLater(new Runnable {
        def run = try { f } catch { case e: Throwable ⇒ log.error("Event thread exception: " + e, e) }
      })
    }
  }
  /** Execute runnable in JavaFX application and return result or exception. */
  def execNGet[T](f: ⇒ T)(implicit duration: JFX.EventLoopRunnableDuration = JFX.ShortRunnable): T = try {
    if (Platform.isFxApplicationThread()) {
      if (JFX.debug) {
        val t = new Throwable(s"Entry point from ${java.lang.Thread.currentThread.getName()}.")
        val ts = System.currentTimeMillis()
        val result = f
        val duration = System.currentTimeMillis() - ts
        if (duration > 500)
          log.error(s"Too heavy operation: ${duration}ms.", t)
        result
      } else f
    } else execNGetAsync({ f })
  } catch {
    case e: Throwable ⇒
      throw new ExecutionException(e)
  }
  /** Asynchronously execute runnable in JavaFX application and return result or exception. */
  def execNGetAsync[T](f: ⇒ T)(implicit duration: JFX.EventLoopRunnableDuration = JFX.ShortRunnable): T = {
    val exchanger = new Exchanger[Either[Throwable, T]]()
    if (duration == JFX.ShortRunnable && JFX.debug) {
      val t = new Throwable(s"Entry point from ${java.lang.Thread.currentThread.getName()}.")
      Platform.runLater(new Runnable {
        def run = {
          val ts = System.currentTimeMillis()
          try exchanger.exchange(Right(f), 100, TimeUnit.MILLISECONDS)
          catch { case e: Throwable ⇒ exchanger.exchange(Left(e), 100, TimeUnit.MILLISECONDS) }
          val duration = System.currentTimeMillis() - ts
          if (duration > 500)
            log.error(s"Too heavy operation: ${duration}ms.", t)
        }
      })
    } else {
      Platform.runLater(new Runnable {
        def run = try exchanger.exchange(Right(f), 100, TimeUnit.MILLISECONDS)
        catch { case e: Throwable ⇒ exchanger.exchange(Left(e), 100, TimeUnit.MILLISECONDS) }
      })
    }
    {
      if (duration == JFX.ShortRunnable)
        exchanger.exchange(null, JFX.timeout, TimeUnit.MILLISECONDS)
      else
        exchanger.exchange(null)
    } match {
      case Left(e) ⇒
        throw e
      case Right(r) ⇒
        r
    }
  }
}
