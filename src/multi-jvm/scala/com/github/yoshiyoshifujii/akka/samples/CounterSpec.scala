package com.github.yoshiyoshifujii.akka.samples

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.ddata.GCounterKey
import akka.cluster.{ Cluster, ClusterEvent }
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{ MultiNodeConfig, MultiNodeSpec }
import akka.testkit.{ ImplicitSender, TestDuration }
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

object CounterSpecConfig extends MultiNodeConfig {
  val node1: RoleName = role("node1")
  val node2: RoleName = role("node2")
  val node3: RoleName = role("node3")

  commonConfig(
    ConfigFactory
      .parseString("""
     |akka.actor.provider = cluster
     |akka.actor.serialization-bindings {
     |  "com.github.yoshiyoshifujii.akka.samples.JsonSerializer" = jackson-cbor
     |}
     |akka.remote.artery.canonical.port = 0
     |akka.cluster.roles = [compute]
     |""".stripMargin).withFallback(ConfigFactory.load())
  )
}

class CounterSpecMultiJvmNode1 extends CounterSpec
class CounterSpecMultiJvmNode2 extends CounterSpec
class CounterSpecMultiJvmNode3 extends CounterSpec

class CounterSpec
    extends MultiNodeSpec(CounterSpecConfig)
    with MultiNodeSpecHelper
    with Matchers
    with ImplicitSender
    with ScalaFutures {
  import CounterSpecConfig._

  override def initialParticipants: Int    = roles.size
  override protected def beforeAll(): Unit = multiNodeSpecBeforeAll()
  override protected def afterAll(): Unit  = multiNodeSpecAfterAll()

  "A Counter spec" must {

    "illustrate how to startup cluster" in within(15.seconds.dilated) {
      Cluster(system).subscribe(testActor, classOf[ClusterEvent.MemberUp])
      expectMsgClass(classOf[ClusterEvent.CurrentClusterState])

      val node1Address = node(node1).address
      val node2Address = node(node2).address
      val node3Address = node(node3).address

      Cluster(system).join(node1Address)

      receiveN(3).collect {
        case ClusterEvent.MemberUp(m) => m.address
      }.toSet should be(
        Set(node1Address, node2Address, node3Address)
      )

      Cluster(system).unsubscribe(testActor)

      testConductor.enter("all-up")
    }

    "Counting up in Node 1 and checking the counted up values in other nodes" in within(15.seconds.dilated) {

      val gCounterKey = GCounterKey("counter-key-1")

      runOn(node1) {
        val counterRef = system.spawn(Counter(gCounterKey), "counter1")

        counterRef ! Counter.Increment

        val probe = TestProbe[Int]()(system.toTyped)
        counterRef ! Counter.GetValue(probe.ref)
        probe.expectMessageType[Int] should be(1)
      }

      runOn(node2, node3) {
        val counterRef = system.spawn(Counter(gCounterKey), "counter2")

        val probe = TestProbe[Int]()(system.toTyped)
        counterRef ! Counter.GetValue(probe.ref)
        probe.expectMessageType[Int] should be(1)
      }

      testConductor.enter("done-1")
    }

  }
}
