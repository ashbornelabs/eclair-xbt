package fr.acinq.eclair.blockchain.bitcoind

import akka.Done
import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import akka.testkit.TestProbe
import com.softwaremill.quicklens.ModifyPimp
import com.ashbornelabs.bitcoin.xbt.BlockHeader
import com.ashbornelabs.bitcoin.xbt.scalacompat.{Block, BlockId, Btc}
import fr.acinq.eclair.blockchain.CurrentBlockHeight
import fr.acinq.eclair.blockchain.bitcoind.ZmqWatcher.{WatchFundingConfirmed, WatchFundingConfirmedTriggered}
import fr.acinq.eclair.blockchain.bitcoind.rpc.BitcoinCoreClient
import fr.acinq.eclair.blockchain.bitcoind.zmq.ZMQActor
import fr.acinq.eclair.{BlockHeight, TestConstants, TestKitBaseClass}
import grizzled.slf4j.Logging
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike
import org.json4s.{JString, JValue}

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise
import scala.concurrent.duration._

class ActivatedKnotsZmqWatcherSpec extends TestKitBaseClass with AnyFunSuiteLike with BitcoindService with BeforeAndAfterAll with Logging {

  private val blake2bActivationHeight = 151
  private var zmqBlock: ActorRef = _
  private var zmqTx: ActorRef = _

  override def beforeAll(): Unit = {
    startXbtKnotsRegtestWithBlake2bActivation(blake2bActivationHeight)
    val (blockConnected, txConnected) = (Promise[Done](), Promise[Done]())
    zmqBlock = system.actorOf(Props(new ZMQActor(s"tcp://127.0.0.1:$bitcoindZmqBlockPort", ZMQActor.Topics.HashBlock, Some(blockConnected))))
    zmqTx = system.actorOf(Props(new ZMQActor(s"tcp://127.0.0.1:$bitcoindZmqTxPort", ZMQActor.Topics.RawTx, Some(txConnected))))
    awaitCond(blockConnected.isCompleted && txConnected.isCompleted)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    system.stop(zmqBlock)
    system.stop(zmqTx)
    stopBitcoind()
    super.afterAll()
  }

  private def serializedHeaderSize(height: Int, probe: TestProbe): Int = {
    val bitcoinClient = new BitcoinCoreClient(bitcoinrpcclient)
    bitcoinClient.getBlockId(height).pipeTo(probe.ref)
    val blockId = probe.expectMsgType[BlockId](10 seconds)
    bitcoinClient.getBlock(blockId).pipeTo(probe.ref)
    val block = probe.expectMsgType[Block](10 seconds)
    BlockHeader.write(block.header).length
  }

  test("process the activation header and reorg without losing confirmation depth") {
    val blockCount = new AtomicLong()
    val probe = TestProbe()
    val heights = TestProbe()
    system.eventStream.subscribe(heights.ref, classOf[CurrentBlockHeight])
    val bitcoinClient = new BitcoinCoreClient(bitcoinrpcclient)
    val nodeParams = TestConstants.Alice.nodeParams
      .modify(_.chainHash).setTo(Block.RegtestGenesisBlock.hash)
      .modify(_.channelConf.scanPreviousBlocksDepth).setTo(0)
    val watcher = system.spawn(ZmqWatcher(nodeParams, blockCount, bitcoinClient), UUID.randomUUID().toString)

    try {
      assert(heights.expectMsgType[CurrentBlockHeight].blockHeight == BlockHeight(150))
      assert(serializedHeaderSize(150, probe) == 80)

      val address = getNewAddress(probe)
      val tx = sendToAddress(address, Btc(0.5), probe)
      watcher ! WatchFundingConfirmed(probe.ref, tx.txid, minDepth = 2)

      generateBlocks(1)
      assert(heights.expectMsgType[CurrentBlockHeight].blockHeight == BlockHeight(blake2bActivationHeight))
      assert(serializedHeaderSize(blake2bActivationHeight, probe) == 164)
      probe.expectNoMessage(100 millis)

      probe.send(bitcoincli, BitcoinReq("getblockhash", blake2bActivationHeight))
      val JString(activationBlockHash) = probe.expectMsgType[JValue]
      probe.send(bitcoincli, BitcoinReq("invalidateblock", activationBlockHash))
      probe.expectMsgType[JValue]

      generateBlocks(1)
      probe.send(bitcoincli, BitcoinReq("getblockhash", blake2bActivationHeight))
      val JString(replacementBlockHash) = probe.expectMsgType[JValue]
      assert(replacementBlockHash != activationBlockHash)
      assert(serializedHeaderSize(blake2bActivationHeight, probe) == 164)
      probe.expectNoMessage(100 millis)

      generateBlocks(1)
      assert(heights.expectMsgType[CurrentBlockHeight].blockHeight == BlockHeight(blake2bActivationHeight + 1))
      assert(probe.expectMsgType[WatchFundingConfirmedTriggered](10 seconds).tx.txid == tx.txid)
    } finally {
      system.stop(watcher)
      system.eventStream.unsubscribe(heights.ref, classOf[CurrentBlockHeight])
    }
  }
}
