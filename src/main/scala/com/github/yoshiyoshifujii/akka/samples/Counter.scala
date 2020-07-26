package com.github.yoshiyoshifujii.akka.samples

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.cluster.ddata.typed.scaladsl.{ DistributedData, Replicator }
import akka.cluster.ddata.{ GCounter, GCounterKey, SelfUniqueAddress }

object Counter {

  sealed trait Command
  case object Increment                                                               extends Command
  final case class GetValue(replyTo: ActorRef[Int])                                   extends Command
  final case class GetCachedValue(replyTo: ActorRef[Int])                             extends Command
  case object Unsubscribe                                                             extends Command
  private sealed trait InternalCommand                                                extends Command
  private case class InternalUpdateResponse(rsp: Replicator.UpdateResponse[GCounter]) extends InternalCommand

  private case class InternalGetResponse(rsp: Replicator.GetResponse[GCounter], replyTo: ActorRef[Int])
      extends InternalCommand
  private case class InternalSubscribeResponse(chg: Replicator.SubscribeResponse[GCounter]) extends InternalCommand

  def apply(key: GCounterKey): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress

      DistributedData.withReplicatorMessageAdapter[Command, GCounter] { replicatorAdapter =>
        replicatorAdapter.subscribe(key, InternalSubscribeResponse.apply)

        def updated(cachedValue: Int): Behavior[Command] =
          Behaviors.receiveMessage {
            case Increment =>
              replicatorAdapter.askUpdate(
                Replicator.Update(key, GCounter.empty, Replicator.WriteLocal)(_ :+ 1),
                InternalUpdateResponse.apply
              )
              Behaviors.same

            case GetValue(replyTo) =>
              replicatorAdapter.askGet(
                Replicator.Get(key, Replicator.ReadLocal),
                value => InternalGetResponse(value, replyTo)
              )
              Behaviors.same

            case GetCachedValue(replyTo) =>
              replyTo ! cachedValue
              Behaviors.same

            case Unsubscribe =>
              replicatorAdapter.unsubscribe(key)
              Behaviors.same

            case internal: InternalCommand =>
              internal match {
                case InternalUpdateResponse(_) =>
                  Behaviors.same

                case InternalGetResponse(rsp @ Replicator.GetSuccess(`key`), replyTo) =>
                  val value = rsp.get(key).value.toInt
                  replyTo ! value
                  Behaviors.same

                case InternalGetResponse(_, _) =>
                  Behaviors.unhandled

                case InternalSubscribeResponse(chg @ Replicator.Changed(`key`)) =>
                  val value = chg.get(key).value.intValue
                  updated(value)

                case InternalSubscribeResponse(Replicator.Deleted(_)) =>
                  Behaviors.unhandled
              }
          }

        updated(cachedValue = 0)
      }

    }

}
