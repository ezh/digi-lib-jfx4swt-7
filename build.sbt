//
// Copyright (c) 2014-2015 Alexey Aksenov ezh@ezh.msk.ru
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// DEVELOPMENT CONFIGURATION

import sbt.application._
import sbt.osgi.manager._

OSGiManager ++ Application // ++ sbt.scct.ScctPlugin.instrumentSettings - ScctPlugin is broken, have no time to fix

name := "digi-lib-jfx4swt-7"

description := "JFX4SWT-7 - Java 7 JavaFX library adapter for SWT framework."

licenses := Seq("GNU Lesser General Public License, Version 3.0" -> url("http://www.gnu.org/licenses/lgpl-3.0.txt"))

organization := "org.digimead"

organizationHomepage := Some(url("http://digimead.org"))

homepage := Some(url("https://github.com/ezh/digi-lib-jfx4swt-7"))

version <<= (baseDirectory) { (b) => scala.io.Source.fromFile(b / "version").mkString.trim }

inConfig(OSGiConf)({
  import OSGiKey._
  Seq(
        osgiBndBundleSymbolicName := "org.digimead.lib.swt4jfx.7",
        osgiBndBundleCopyright := "Copyright © 2014-2015 Alexey B. Aksenov/Ezh. All rights reserved.",
        osgiBndBundleLicense := "http://www.gnu.org/licenses/lgpl-3.0.txt;description=GNU Lesser General Public License, Version 3.0",
        osgiBndExportPackage := List("org.digimead.digi.lib.*"),
        osgiBndImportPackage := List("!org.aspectj.*", "*"),
        osgiBndPrivatePackage := List(),
        osgiBndBundleFragmentHost := "org.digimead.lib.swt4jfx",
        osgiBndRequireCapability := """osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.7)(!(version>=1.8)))"""")
})

crossScalaVersions := Seq("2.11.6")

scalaVersion := "2.11.6"

scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-Xcheckinit", "-feature", "-Xelide-below", "ALL")

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")


//
// Custom local options
//

resolvers += "swt-repo" at "https://swt-repo.googlecode.com/svn/repo/"

ivyConfigurations += config("compileonly").hide

unmanagedClasspath in Compile ++= update.value.select(configurationFilter("compileonly"))

resolvers += "digimead-maven" at "http://storage.googleapis.com/maven.repository.digimead.org/"

libraryDependencies ++= {
  val os = (sys.props("os.name"), sys.props("os.arch")) match {
    case ("Linux", "amd64") => "gtk.linux.x86_64"
    case ("Linux", _) => "gtk.linux.x86"
    case ("Mac OS X", "amd64" | "x86_64") => "cocoa.macosx.x86_64"
    case ("Mac OS X", _) => "cocoa.macosx.x86"
    case (os, "amd64") if os.startsWith("Windows") => "win32.win32.x86_64"
    case (os, _) if os.startsWith("Windows") => "win32.win32.x86"
    case (os, arch) => sys.error("Cannot obtain lib for OS '" + os + "' and architecture '" + arch + "'")
  }
  val artifact = "org.eclipse.swt." + os
  Seq(
    "org.eclipse.swt" % artifact % "4.2.1" % "compileonly",
    "org.eclipse.swt" % artifact % "4.2.1" % "test",
    "org.digimead" %% "digi-lib-jfx4swt" % "0.1.0.9",
    "org.digimead" %% "digi-lib-test" % "0.3.1.3" % "test"
  )
}

//
// Testing
//

parallelExecution in Test := false

testGrouping in Test <<= (definedTests in Test) map { tests =>
  tests map { test =>
    new Tests.Group(
      name = test.name,
      tests = Seq(test),
      runPolicy = Tests.SubProcess(ForkOptions(runJVMOptions = Seq.empty[String])))
  }
}

//logLevel := Level.Debug
