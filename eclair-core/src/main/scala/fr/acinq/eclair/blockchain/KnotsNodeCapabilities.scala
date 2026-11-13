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

import org.json4s.JValue

/** XBT/Knots capability data returned by the node during startup preflight. */
case class KnotsNodeCapabilities(subversion: String,
                                 blake2bActivationHeight_opt: Option[Long],
                                 rdtsActivationHeight_opt: Option[Long],
                                 rdtsExpiryTime_opt: Option[Long]) {
  /** Knots adds this marker to the local subversion returned by getnetworkinfo. */
  def isKnotsBuild: Boolean = subversion.contains("Knots:")

  /** Return every mismatch between the node's advertised fork schedule and the selected XBT profile. */
  def compatibilityErrors(profile: KnotsCompatibilityProfile): Seq[String] = {
    val expectedBlake2bHeight_opt =
      if (profile.consensus.blake2bHeight == Int.MaxValue) None else Some(profile.consensus.blake2bHeight.toLong)
    val expectedRdtsHeight_opt = expectedBlake2bHeight_opt
    val expectedRdtsExpiryTime_opt =
      if (profile.consensus.rdtsExpiryTime == Long.MinValue) None else Some(profile.consensus.rdtsExpiryTime)

    Seq(
      Option.when(!isKnotsBuild)(s"node subversion does not identify a Knots build (subversion=$subversion)"),
      Option.when(blake2bActivationHeight_opt != expectedBlake2bHeight_opt)(s"BLAKE2b activation height mismatch (expected=$expectedBlake2bHeight_opt, bitcoind=$blake2bActivationHeight_opt)"),
      Option.when(rdtsActivationHeight_opt != expectedRdtsHeight_opt)(s"reduced-data activation height mismatch (expected=$expectedRdtsHeight_opt, bitcoind=$rdtsActivationHeight_opt)"),
      Option.when(rdtsExpiryTime_opt != expectedRdtsExpiryTime_opt)(s"reduced-data expiry time mismatch (expected=$expectedRdtsExpiryTime_opt, bitcoind=$rdtsExpiryTime_opt)")
    ).flatten
  }
}

object KnotsNodeCapabilities {

  implicit val formats: org.json4s.Formats = org.json4s.DefaultFormats

  /** Extract the Knots identity and XBT deployment fields from their startup RPC responses. */
  def fromRpc(networkInfo: JValue, deploymentInfo: JValue): KnotsNodeCapabilities = {
    val subversion = (networkInfo \ "subversion").extract[String]
    val blake2bActivationHeight_opt = (deploymentInfo \ "blake2b" \ "height").extractOpt[Long]
    val rdtsDeployment = deploymentInfo \ "deployments" \ "reduced_data"
    val rdtsActivationHeight_opt = (rdtsDeployment \ "height").extractOpt[Long]
    val rdtsExpiryTime_opt = (rdtsDeployment \ "expiry_time").extractOpt[Long]
    KnotsNodeCapabilities(subversion, blake2bActivationHeight_opt, rdtsActivationHeight_opt, rdtsExpiryTime_opt)
  }
}
