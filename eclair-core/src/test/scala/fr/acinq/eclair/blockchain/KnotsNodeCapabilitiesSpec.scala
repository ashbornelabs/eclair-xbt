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

import org.json4s.jackson.JsonMethods.parse
import org.scalatest.funsuite.AnyFunSuite

class KnotsNodeCapabilitiesSpec extends AnyFunSuite {

  test("parse Knots identity and XBT deployment fields from startup RPC responses") {
    val networkInfo = parse("""{"subversion":"/Knots:29.4.3/"}""")
    val deploymentInfo = parse("""{"blake2b":{"height":961640,"active":false},"deployments":{"reduced_data":{"height":961640,"expiry_time":1819756800,"active":false}}}""")

    assert(KnotsNodeCapabilities.fromRpc(networkInfo, deploymentInfo) == KnotsNodeCapabilities(
      subversion = "/Knots:29.4.3/",
      blake2bActivationHeight_opt = Some(961640L),
      rdtsActivationHeight_opt = Some(961640L),
      rdtsExpiryTime_opt = Some(1819756800L)
    ))
  }

  test("accept the pinned mainnet and testnet4 Knots schedules") {
    val mainnet = KnotsNodeCapabilities("/Knots:29.4.3/", Some(961640L), Some(961640L), Some(1819756800L))
    val testnet4 = KnotsNodeCapabilities("/Knots:29.4.3/", Some(150308L), Some(150308L), Some(1806537600L))

    assert(mainnet.compatibilityErrors(KnotsCompatibilityProfile.Mainnet).isEmpty)
    assert(testnet4.compatibilityErrors(KnotsCompatibilityProfile.Testnet4).isEmpty)
  }

  test("accept default regtest when XBT consensus changes are unscheduled") {
    val regtest = KnotsNodeCapabilities("/Knots:29.4.3/", None, None, None)

    assert(regtest.compatibilityErrors(KnotsCompatibilityProfile.Regtest).isEmpty)
  }

  test("report non-Knots identity and every mismatched deployment value") {
    val capabilities = KnotsNodeCapabilities("/Satoshi:31.0/", Some(123L), Some(456L), Some(789L))

    val errors = capabilities.compatibilityErrors(KnotsCompatibilityProfile.Mainnet)

    assert(errors.size == 4)
    assert(errors.exists(_.contains("does not identify a Knots build")))
    assert(errors.exists(_.contains("BLAKE2b activation height mismatch")))
    assert(errors.exists(_.contains("reduced-data activation height mismatch")))
    assert(errors.exists(_.contains("reduced-data expiry time mismatch")))
  }

  test("reject a regtest node that reports a scheduled XBT fork") {
    val capabilities = KnotsNodeCapabilities("/Knots:29.4.3/", Some(100L), Some(100L), Some(200L))

    assert(capabilities.compatibilityErrors(KnotsCompatibilityProfile.Regtest).size == 3)
  }
}
