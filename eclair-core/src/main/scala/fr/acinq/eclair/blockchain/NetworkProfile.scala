/*
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

package fr.acinq.eclair.blockchain

import com.ashbornelabs.bitcoin.xbt.scalacompat.{Block, BlockHash}

/**
 * The identity and encoding parameters of an Eclair network.
 *
 * Bitcoin and Lightning deliberately expose separate fields here. XBT currently
 * uses the same genesis hash for both identities, but keeping the concepts
 * separate prevents Bitcoin Core validation details from being implicitly used
 * as the Lightning protocol identity.
 *
 * @param name                 canonical Eclair network name
 * @param bitcoinGenesisHash   genesis hash expected from Bitcoin Core
 * @param lightningChainHash   chain hash used in Lightning protocol messages
 * @param bitcoinCoreChain     Bitcoin Core chain identifier
 * @param bolt11Prefix         BOLT 11 human-readable prefix
 */
case class NetworkProfile(name: String,
                          bitcoinGenesisHash: BlockHash,
                          lightningChainHash: BlockHash,
                          bitcoinCoreChain: String,
                          bolt11Prefix: String)

object NetworkProfile {
  val Mainnet: NetworkProfile = NetworkProfile(
    name = "mainnet",
    bitcoinGenesisHash = Block.LivenetGenesisBlock.hash,
    lightningChainHash = Block.LivenetGenesisBlock.hash,
    bitcoinCoreChain = "main",
    bolt11Prefix = "lnbc"
  )

  val Testnet4: NetworkProfile = NetworkProfile(
    name = "testnet4",
    bitcoinGenesisHash = Block.Testnet4GenesisBlock.hash,
    lightningChainHash = Block.Testnet4GenesisBlock.hash,
    bitcoinCoreChain = "testnet4",
    bolt11Prefix = "lntb"
  )

  val Regtest: NetworkProfile = NetworkProfile(
    name = "regtest",
    bitcoinGenesisHash = Block.RegtestGenesisBlock.hash,
    lightningChainHash = Block.RegtestGenesisBlock.hash,
    bitcoinCoreChain = "regtest",
    bolt11Prefix = "lnbcrt"
  )

  val values: Seq[NetworkProfile] = Seq(Mainnet, Testnet4, Regtest)

  private val profilesByName: Map[String, NetworkProfile] = Map(
    Mainnet.name -> Mainnet,
    Testnet4.name -> Testnet4,
    "testnet" -> Testnet4,
    Regtest.name -> Regtest
  )

  private val profilesByBitcoinGenesisHash: Map[BlockHash, NetworkProfile] = values.map(profile => profile.bitcoinGenesisHash -> profile).toMap
  private val profilesByLightningChainHash: Map[BlockHash, NetworkProfile] = values.map(profile => profile.lightningChainHash -> profile).toMap

  def fromName(name: String): Option[NetworkProfile] = profilesByName.get(name)

  def fromBitcoinGenesisHash(hash: BlockHash): Option[NetworkProfile] = profilesByBitcoinGenesisHash.get(hash)

  def fromLightningChainHash(hash: BlockHash): Option[NetworkProfile] = profilesByLightningChainHash.get(hash)
}
