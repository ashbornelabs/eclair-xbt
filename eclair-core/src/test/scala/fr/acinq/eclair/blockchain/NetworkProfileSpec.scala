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

import com.ashbornelabs.bitcoin.xbt.scalacompat.{Block, BlockHash, ByteVector32}
import fr.acinq.eclair.NodeParams
import org.scalatest.funsuite.AnyFunSuite
import scodec.bits.ByteVector

class NetworkProfileSpec extends AnyFunSuite {

  test("supported profiles keep Bitcoin and Lightning identities explicit") {
    val profiles = Seq(NetworkProfile.Mainnet, NetworkProfile.Testnet4, NetworkProfile.Regtest)

    assert(profiles.map(_.name) == Seq("mainnet", "testnet4", "regtest"))
    assert(profiles.forall(profile => profile.bitcoinGenesisHash == profile.lightningChainHash))
    assert(NetworkProfile.Mainnet.bitcoinGenesisHash == Block.LivenetGenesisBlock.hash)
    assert(NetworkProfile.Testnet4.bitcoinGenesisHash == Block.Testnet4GenesisBlock.hash)
    assert(NetworkProfile.Regtest.bitcoinGenesisHash == Block.RegtestGenesisBlock.hash)
  }

  test("profiles resolve canonical names and the legacy testnet alias") {
    assert(NetworkProfile.fromName("mainnet") == Some(NetworkProfile.Mainnet))
    assert(NetworkProfile.fromName("testnet") == Some(NetworkProfile.Testnet4))
    assert(NetworkProfile.fromName("testnet4") == Some(NetworkProfile.Testnet4))
    assert(NetworkProfile.fromName("regtest") == Some(NetworkProfile.Regtest))
    assert(NetworkProfile.fromName("unknown").isEmpty)
  }

  test("profiles resolve both Bitcoin and Lightning identities") {
    NetworkProfile.values.foreach { profile =>
      assert(NetworkProfile.fromBitcoinGenesisHash(profile.bitcoinGenesisHash) == Some(profile))
      assert(NetworkProfile.fromLightningChainHash(profile.lightningChainHash) == Some(profile))
    }

    val unknownHash = BlockHash(ByteVector32(ByteVector.fill(32)(0x42)))
    assert(NetworkProfile.fromBitcoinGenesisHash(unknownHash).isEmpty)
    assert(NetworkProfile.fromLightningChainHash(unknownHash).isEmpty)
  }

  test("NodeParams uses the profile Lightning identity") {
    assert(NodeParams.hashFromChain("mainnet") == NetworkProfile.Mainnet.lightningChainHash)
    assert(NodeParams.hashFromChain("testnet") == NetworkProfile.Testnet4.lightningChainHash)
    assert(NodeParams.hashFromChain("regtest") == NetworkProfile.Regtest.lightningChainHash)
    assert(NodeParams.chainFromHash(NetworkProfile.Testnet4.lightningChainHash) == "testnet4")
  }
}
