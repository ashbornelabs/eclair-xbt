package fr.acinq.eclair.integration

import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
import akka.testkit.TestProbe
import com.ashbornelabs.bitcoin.xbt.scalacompat.{ByteVector32, SatoshiLong}
import com.typesafe.config.ConfigFactory
import fr.acinq.eclair.TestUtils
import fr.acinq.eclair.balance.ChannelsListener
import fr.acinq.eclair.channel._
import fr.acinq.eclair.db.Databases
import fr.acinq.eclair.MilliSatoshiLong

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class KnotsRestartRecoveryIntegrationSpec extends IntegrationSpec {

  test("an Eclair node restores its XBT channel and resumes chain monitoring after restart") {
    val configA = nodeConfig("A")
    val configB = nodeConfig("B")
    instantiateEclairNode("A", configA)
    instantiateEclairNode("B", configB)

    val stateListenerA = TestProbe()
    val stateListenerB = TestProbe()
    nodes("A").system.eventStream.subscribe(stateListenerA.ref, classOf[ChannelStateChanged])
    nodes("B").system.eventStream.subscribe(stateListenerB.ref, classOf[ChannelStateChanged])

    val channel = connect(nodes("A"), nodes("B"), 5000000 sat, 0 msat, ChannelTypes.AnchorOutputsZeroFeeHtlcTx())
    generateBlocks(8)
    awaitChannelState(stateListenerA, channel.channelId)
    awaitChannelState(stateListenerB, channel.channelId)

    val nodeIdBeforeRestart = nodes("A").nodeParams.nodeId
    val nodeABeforeRestart = nodes("A")
    akka.testkit.TestKit.shutdownActorSystem(nodeABeforeRestart.system)
    nodeABeforeRestart.nodeParams.db match {
      case db: Databases.SqliteDatabases =>
        db.channels.sqlite.close()
        db.network.sqlite.close()
        db.audit.sqlite.close()
      case other => fail(s"expected SQLite databases, got ${other.getClass.getSimpleName}")
    }
    nodes = nodes - "A"

    // Advance the chain while Eclair A is stopped, then verify that it catches up on startup.
    generateBlocks(1)
    val expectedHeight = currentBlockHeight()

    instantiateEclairNode("A", configA)
    assert(nodes("A").nodeParams.nodeId == nodeIdBeforeRestart)
    awaitRestoredChannel(channel.channelId)
    awaitCond(nodes("A").nodeParams.currentBlockHeight >= expectedHeight, max = 60 seconds, interval = 1 second)

    val restartedStateListenerA = TestProbe()
    nodes("A").system.eventStream.subscribe(restartedStateListenerA.ref, classOf[ChannelStateChanged])
    connect(nodes("A"), nodes("B"))
    awaitChannelState(restartedStateListenerA, channel.channelId)
    awaitChannelState(stateListenerB, channel.channelId)
  }

  private def nodeConfig(alias: String) = {
    val port = TestUtils.availablePort
    ConfigFactory.parseMap(Map(
      "eclair.node-alias" -> alias,
      "eclair.server.port" -> port,
      "eclair.api.port" -> TestUtils.availablePort
    ).asJava).withFallback(withAnchorOutputsZeroFeeHtlcTxs).withFallback(commonConfig)
  }

  private def awaitRestoredChannel(channelId: ByteVector32): Unit = {
    val probe = TestProbe()
    awaitCond({
      nodes("A").channelsListener ! ChannelsListener.GetChannels(probe.ref.toTyped[ChannelsListener.GetChannelsResponse])
      probe.receiveOne(1.second) match {
        case response: ChannelsListener.GetChannelsResponse => response.channels.contains(channelId)
        case _ => false
      }
    }, max = 30 seconds, interval = 1 second)
  }

  private def awaitChannelState(listener: TestProbe, channelId: ByteVector32): Unit = {
    awaitCond({
      listener.receiveOne(1.second) match {
        case state: ChannelStateChanged => state.channelId == channelId && state.currentState == NORMAL
        case _ => false
      }
    }, max = 60 seconds, interval = 1 second)
  }
}
