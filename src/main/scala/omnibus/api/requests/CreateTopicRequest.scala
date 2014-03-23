package omnibus.api.request

import akka.actor._

import spray.routing._
import spray.json._
import spray.http._
import HttpHeaders._

import DefaultJsonProtocol._

import omnibus.api.endpoint.JsonSupport._
import omnibus.domain.topic._
import omnibus.domain.topic.TopicProtocol._
import omnibus.domain.topic.TopicRepositoryProtocol._

class CreateTopicRequest(topicPath: TopicPath, ctx : RequestContext, topicRepo: ActorRef) extends RestRequest(ctx) {

  val prettyTopic = topicPath.prettyStr()

  topicRepo ! TopicRepositoryProtocol.LookupTopic(topicPath)

  override def receive = waitingLookup orElse handleTimeout

  def waitingAck : Receive = {
    case TopicCreated(topicRef) => {
      ctx.complete (StatusCodes.Created, Location(ctx.request.uri):: Nil, s"Topic $prettyTopic created \n")
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
     = Props(classOf[CreateTopicRequest], topicPath, ctx, topicRepo).withDispatcher("requests-dispatcher")
}