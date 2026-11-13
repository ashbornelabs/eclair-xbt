/*
 * Copyright 2020 ACINQ SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.acinq.eclair.blockchain.watchdogs

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.ashbornelabs.bitcoin.xbt.BlockHeader
import fr.acinq.eclair.{BlockHeight, NodeParams}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

/**
 * Created by t-bast on 29/09/2020.
 */

/** External watchdog checks are disabled until XBT-compatible header sources are available. */
object BlockchainWatchdog {

  // @formatter:off
  case class BlockHeaderAt(blockHeight: BlockHeight, blockHeader: BlockHeader)
  case object NoBlockReceivedTimer

  sealed trait BlockchainWatchdogEvent
  /**
   * We are missing too many blocks compared to one of our blockchain watchdogs.
   * We may be eclipsed from the bitcoin network.
   *
   * @param recentHeaders headers fetched from the watchdog blockchain source.
   */
  case class DangerousBlocksSkew(recentHeaders: LatestHeaders) extends BlockchainWatchdogEvent

  sealed trait Command
  case class LatestHeaders(currentBlockHeight: BlockHeight, blockHeaders: Set[BlockHeaderAt], source: String) extends Command
  private[watchdogs] case class WrappedCurrentBlockHeight(currentBlockHeight: BlockHeight) extends Command
  private case class CheckLatestHeaders(currentBlockHeight: BlockHeight) extends Command
  private case class NoBlockReceivedSince(lastBlockHeight: BlockHeight) extends Command
  // @formatter:on

  /** Ignore configured external sources because the built-in sources serve Bitcoin, not XBT. */
  def apply(nodeParams: NodeParams, maxRandomDelay: FiniteDuration, blockTimeout: FiniteDuration = 15 minutes): Behavior[Command] = {
    Behaviors.setup { context =>
      if (nodeParams.blockchainWatchdogSources.nonEmpty) {
        context.log.warn("ignoring external blockchain watchdog sources: no XBT-compatible sources are available")
      } else {
        context.log.info("external blockchain watchdog is disabled: no XBT-compatible sources are available")
      }
      Behaviors.ignore[Command]
    }
  }

}
