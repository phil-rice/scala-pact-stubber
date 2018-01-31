package com.ing.pact.stubber

import java.io.{File, FileFilter}
import javax.net.ssl.SSLContext

import com.itv.scalapact.shared.SslContextMap
import com.typesafe.config.Config

import scala.collection.JavaConverters._

trait FromConfig[T] {
  def apply(config: Config): T
}

trait FromConfigWithKey[T] {
  def apply(name: String, config: Config): T
}

object FromConfig {

  implicit object FromConfigForSslContext extends FromConfig[SSLContext] {
    override def apply(config: Config): SSLContext = SslContextMap.makeSslContext(config.getString("keystore"), config.getString("keystore-password"), config.getString("truststore"), config.getString("truststore-password"))
  }

}

trait Pimpers {
  def printMsg[X](msg: X => String) = { x: X => println(msg(x)); x }

  implicit class AnyPimper[T](t: T) {
    def ==>[T1](fn: T => T1) = fn(t)
  }

  implicit class FnPimper[From, To](fn: From => To) {
    def ===>[T1](fn2: To => T1) = fn andThen fn2

    //    def =+->[NewTo](fn2: From => To => NewTo) ={ from: From => fn2(from)(fn(from)) }

  }

  implicit class SeqPimper[T](seq: Seq[T]) {
    def ===>[T1](fn: T => T1): Seq[T1] = seq.map(fn)
    def =+>[T1](fn: T => (T => T1)): Seq[T1] = mapWith(fn)
    def mapWith[T1](fn: T => (T => T1)): Seq[T1] = seq.map(t => fn(t)(t))
    def printWithTitle(title: String): Unit = {
      if (!seq.isEmpty) println(title)
      seq.foreach(println)
    }

  }

  implicit class SeqTuplePimper[L, R](seq: Seq[(L, R)]) {
    def =+>[T1](fn: L => R => T1) = seq.map { case (l, r) => fn(l)(r) }
  }

  implicit class SeqEitherPimper[L, R](seq: Seq[Either[L, R]]) {
    def issues: Seq[L] = seq.collect { case Left(l) => l }
    def values: Seq[R] = seq.collect { case Right(r) => r }
  }

  def printIssuesAndReturnvalues[L, R](title: String)(seq: Seq[Either[L, R]]): Seq[R] = {
    seq.issues.printWithTitle(title)
    seq.values
  }

  implicit class ConfigPimper(config: Config) {
    def getOption[A](name: String)(implicit fromConfig: FromConfig[A]): Option[A] = if (config.hasPath(name)) Some(fromConfig(config.getConfig(name))) else None
    def getFiles(name: String)(filenameFilter: FileFilter) = {
      val fileName = config.getString(name)
      if (fileName == null) throw new NullPointerException(s"Cannot load file $name")
      val directory = new File(fileName)
      if (!directory.isDirectory) throw new IllegalArgumentException(s"Filename $name is $fileName and that isn't a directory")
      directory.listFiles(filenameFilter).toList
    }
    def get[A](name: String)(implicit fromConfig: FromConfig[A]) = fromConfig(config.getConfig(name))
    def mapList[A](name: String)(implicit fromConfig: FromConfigWithKey[A]): List[A] = config.getObject(name).keySet().asScala.toList.sorted.map(key => fromConfig(name, config.getConfig(name).getConfig(key)))
  }

  def fromConfig(name: String)(config: Config) = (config, name)
  def makeListFromConfig[A: FromConfigWithKey](key: String)(config: Config) = config.mapList(key)

}
