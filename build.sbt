// Slightly complicated build file for use with pfn's excellent
// Android Scala sbt plugin.
//
// Please see here for details:
// https://github.com/pfn/android-sdk-plugin/blob/master/README.md

import android.Keys._

android.Plugin.androidBuild

name := "hello-xmpp-scala"

version := "0.1.0"

scalacOptions in Compile ++= Seq("-feature", "-unchecked", "-deprecation")

platformTarget in Android := "android-19"

libraryDependencies ++= Seq(
  "org.robolectric" % "robolectric" % "2.3" % "test",
  "junit" % "junit" % "4.11" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.1.RC1" % "test",
  "org.scalatest" % "scalatest_2.10" % "2.2.1" % "test",
  "com.netflix.rxjava" % "rxjava-core" % "0.20.4",
  "com.netflix.rxjava" % "rxjava-scala" % "0.20.4",
  "com.netflix.rxjava" % "rxjava-android" % "0.20.4",
//  "org.igniterealtime.smack" % "smack-java7" % "4.1.0-alpha1",
  "org.igniterealtime.smack" % "smack-android" % "4.1.0-alpha5",
  "org.igniterealtime.smack" % "smack-tcp" % "4.1.0-alpha5",
//  "org.igniterealtime.smack" % "smack-core" % "4.1.0-alpha5",
  "org.igniterealtime.smack" % "smack-android-extensions" % "4.1.0-alpha5"
//  "org.igniterealtime.smack" % "smack-bosh" % "4.1.0-alpha5",
)

// Make the actually targeted Android jars available to Robolectric for shadowing.
managedClasspath in Test <++= (platformJars in Android, baseDirectory) map {
  (j, b) => Seq(Attributed.blank(b / "bin" / "classes"), Attributed.blank(file(j._1)))
}

// With this option, we cannot have dependencies in the test scope!
debugIncludesTests in Android := false

exportJars in Test := false

// Supress warnings so that Proguard will do its job.
proguardOptions in Android ++= Seq(
  "-keep class org.xmlpull.v1.*",
  "-keep class javax.net.ssl.**",
  "-keep class org.jivesoftware.**",
  "-dontwarn org.xmlpull.**",
  "-dontwarn rx.internal.util.**",
  "-dontwarn android.test.**"
)

// Required so Proguard won't remove the actual instrumentation tests.
proguardOptions in Android ++= Seq(
  "-keep public class * extends junit.framework.TestCase",
  "-keepclassmembers class * extends junit.framework.TestCase { *; }"
)

apkbuildExcludes in Android ++= Seq(
  "LICENSE.txt",
  "META-INF/DEPENDENCIES",
  "META-INF/LICENSE",
  "META-INF/LICENSE.txt",
  "META-INF/NOTICE",
  "META-INF/NOTICE.txt"
)
