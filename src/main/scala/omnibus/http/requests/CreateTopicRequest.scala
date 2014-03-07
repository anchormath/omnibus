package omnibus.http.request

import akka.pattern._
import akka.actor._
import akka.actor.Status._

import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding._
import spray.routing._
import spray.routing.authentication._
import spray.json._
import spray.httpx.marshalling._
import spray.http._
import HttpHeaders._
import MediaTypes._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import DefaultJsonProtocol._
import reflect.ClassTag

import omnibus.http.JsonSupport._
import omnibus.http.streaming._
import omnibus.configuration._
import omnibus.domain._
import omnibus.domain.topic._
import omnibus.domain.subscriber._
import omnibus.repository._

class CreateTopicRequest(topicPath: TopicPath, ctx : RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  val prettyTopic = topicPath.prettyStr()

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = waitingLookup orElse handleTimeout

  def waitingAck : Receive = {
    case true        => {
      ctx.complete (StatusCodes.Created, Location(ctx.request.uri):: Nil, s"Topic $prettyTopic created \n")
      self ! PoisonPill
    }  
    case Failure(ex) => {
      ctx.complete(ex)
      self ! PoisonPill
    }  
  }

  def waitingLookup : Receive = {
    case TopicPathRef(topicPath, optRef) => handleTopicPathRef(topicPath, optRef)
  }

  def handleTopicPathRef(topicPath: TopicPath, topicRef : Option[ActorRef]) = topicRef match {
    case Some(ref) => {
      ctx.complete(new TopicAlreadyExistsException(topicPath.prettyStr()))
      self ! PoisonPill
    }
    case None      => {
      topicRepo ! TopicRepositoryProtocol.CreateTopic(topicPath)
      context.become(waitingAck orElse handleTimeout)
    }  
  }
}

object CreateTopicRequest {
   def props(topicPath: TopicPath, ctx : RequestContext, topicRepo: ActorRef) 
     = Props(classOf[CreateTopicRequest], topicPath, ctx, topicRepo)
}