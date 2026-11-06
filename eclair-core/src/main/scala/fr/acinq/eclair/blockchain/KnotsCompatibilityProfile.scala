/*
 * Copyright 2026 Ashborne Labs
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

package fr.acinq.eclair.blockchain

import com.ashbornelabs.bitcoin.xbt.ConsensusParams

/**
 * The XBT consensus and startup-RPC policy derived from the pinned Bitcoin
 * Knots reference implementation.
 *
 * This policy is intentionally separate from the Bitcoin Core node version:
 * compatibility depends on the active chain profile and its consensus rules,
 * not only on the software's reported version number. Runtime validation is
 * added separately once these policy values are wired into startup.
 */
case class KnotsCompatibilityProfile(networkProfile: NetworkProfile,
                                     consensus: ConsensusParams,
                                     requiredRpcMethods: Set[String])

object KnotsCompatibilityProfile {
  /** RPC methods needed by Eclair's startup Bitcoin Core preflight. */
  val RequiredStartupRpcMethods: Set[String] = Set(
    "getblockchaininfo",
    "getblockhash",
    "getnetworkinfo",
    "getrawtransaction",
    "listwallets",
    "listunspent"
  )

  val Mainnet: KnotsCompatibilityProfile = KnotsCompatibilityProfile(
    networkProfile = NetworkProfile.Mainnet,
    consensus = ConsensusParams.Mainnet,
    requiredRpcMethods = RequiredStartupRpcMethods
  )

  val Testnet4: KnotsCompatibilityProfile = KnotsCompatibilityProfile(
    networkProfile = NetworkProfile.Testnet4,
    consensus = ConsensusParams.Testnet4,
    requiredRpcMethods = RequiredStartupRpcMethods
  )

  val Regtest: KnotsCompatibilityProfile = KnotsCompatibilityProfile(
    networkProfile = NetworkProfile.Regtest,
    consensus = ConsensusParams.Regtest,
    requiredRpcMethods = RequiredStartupRpcMethods
  )

  val values: Seq[KnotsCompatibilityProfile] = Seq(Mainnet, Testnet4, Regtest)

  private val profilesByNetwork: Map[NetworkProfile, KnotsCompatibilityProfile] =
    values.map(profile => profile.networkProfile -> profile).toMap

  def forNetwork(networkProfile: NetworkProfile): KnotsCompatibilityProfile =
    profilesByNetwork.getOrElse(networkProfile, throw new RuntimeException(s"unsupported network profile '$networkProfile'"))
}
