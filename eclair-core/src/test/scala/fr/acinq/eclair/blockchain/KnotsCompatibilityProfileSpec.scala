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

import org.scalatest.funsuite.AnyFunSuite

class KnotsCompatibilityProfileSpec extends AnyFunSuite {

  test("profiles match the pinned Knots consensus parameters") {
    assert(KnotsCompatibilityProfile.Mainnet.consensus.blake2bHeight == 961640)
    assert(KnotsCompatibilityProfile.Mainnet.consensus.blake2bTargetShift == 22)
    assert(KnotsCompatibilityProfile.Mainnet.consensus.rdtsExpiryTime == 1819756800L)

    assert(KnotsCompatibilityProfile.Testnet4.consensus.blake2bHeight == 150308)
    assert(KnotsCompatibilityProfile.Testnet4.consensus.blake2bTargetShift == 20)
    assert(KnotsCompatibilityProfile.Testnet4.consensus.rdtsExpiryTime == 1806537600L)
  }

  test("regtest keeps the Knots fork unscheduled by default") {
    assert(KnotsCompatibilityProfile.Regtest.consensus.blake2bHeight == Int.MaxValue)
    assert(KnotsCompatibilityProfile.Regtest.consensus.blake2bTargetShift == 20)
    assert(KnotsCompatibilityProfile.Regtest.consensus.rdtsExpiryTime == Long.MinValue)
  }

  test("profiles resolve from the configured network profile") {
    assert(KnotsCompatibilityProfile.forNetwork(NetworkProfile.Mainnet) == KnotsCompatibilityProfile.Mainnet)
    assert(KnotsCompatibilityProfile.forNetwork(NetworkProfile.Testnet4) == KnotsCompatibilityProfile.Testnet4)
    assert(KnotsCompatibilityProfile.forNetwork(NetworkProfile.Regtest) == KnotsCompatibilityProfile.Regtest)
  }

  test("profiles declare the startup RPC capability set") {
    val expected = Set(
      "getblockchaininfo",
      "getblockhash",
      "getnetworkinfo",
      "getrawtransaction",
      "listwallets",
      "listunspent"
    )

    assert(KnotsCompatibilityProfile.RequiredStartupRpcMethods == expected)
    assert(KnotsCompatibilityProfile.values.forall(_.requiredRpcMethods == expected))
  }
}
