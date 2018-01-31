package com.ing.pact.stubber

import java.io.File
import javax.net.ssl.SSLContext

import com.itv.scalapact.shared.ColourOuput._
import com.itv.scalapact.shared._
import com.itv.scalapact.shared.http.PactStubService._
import com.itv.scalapactcore.common.PactReaderWriter._
import com.itv.scalapactcore.stubber.InteractionManager
import com.typesafe.config.{Config, ConfigFactory}
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.io.Source

case class ServerSpec(name: String, port: Int, strict: Boolean, sslContext: Option[SSLContext], pacts: List[File])


object ServerSpec {

  implicit object FromConfigForServerSpec extends FromConfigWithKey[ServerSpec] with Pimpers {
    override def apply(name: String, config: Config): ServerSpec =
      ServerSpec(name,
        port = config.getInt("port"),
        strict = false,
        sslContext = config.getOption[SSLContext]("ssl-context"),
        pacts = config.getFiles("directory")(_.getName.endsWith(".json")))

  }

  implicit class ServerSpecPimper(spec: ServerSpec)(implicit executionContext: ExecutionContext) extends Pimpers {
    def toBlaizeServer(pacts: Seq[Pact]) = {
      val interactionManager = pacts.foldLeft(new InteractionManager) { (im, t) => t.interactions ==> im.addInteractions; im }
      BlazeBuilder
        .bindHttp(spec.port, "localhost")
        .withExecutionContext(executionContext)
        .withIdleTimeout(60.seconds)
        .withOptionalSsl(spec.sslContext)
        .withConnectorPoolSize(10)
        .mountService(ServiceMaker.service(interactionManager, spec.strict), "/")
        .run
    }

  }

  def loadPacts: ServerSpec => List[Either[String, Pact]] = { serverSpec: ServerSpec => serverSpec.pacts.map(Source.fromFile(_, "UTF-8").mkString).map(pactReader.jsonStringToPact) }

}

case class ServerSpecAndPacts(spec: ServerSpec, issuesAndPacts: List[Either[String, Pact]])


object ServerSpecAndPacts extends Pimpers {
  def printIssuesAndReturnPacts(title: String)(serverSpecAndPacts: ServerSpecAndPacts): Seq[Pact] = {
    serverSpecAndPacts.issuesAndPacts.printWithTitle(title)
    serverSpecAndPacts.issuesAndPacts.values
  }
}

object Stubber extends App with Pimpers {
  PactLogger.message("*************************************".white.bold)
  PactLogger.message("** ScalaPact: Running Stubber      **".white.bold)
  PactLogger.message("*************************************".white.bold)


  def printPactData[L, R](spec: ServerSpec) = printIssuesAndReturnvalues[L, R](s"Issues loading server ${spec.name}") ===> printMsg(pacts => s"Starting on Port ${spec.port} with ${pacts.length} pacts")

  new File("stubber.cfg") ==> ConfigFactory.parseFile ==> makeListFromConfig[ServerSpec](key = "servers") mapWith { spec => ServerSpec.loadPacts ===> printPactData(spec) ===> spec.toBlaizeServer }

  while (true)
    Thread.sleep(10000000)

}
