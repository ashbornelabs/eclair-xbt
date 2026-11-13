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

import com.ashbornelabs.bitcoin.xbt.BlockHeader
import org.scalatest.funsuite.AnyFunSuite
import scodec.bits.HexStringSyntax

class BlockHeaderParserSpec extends AnyFunSuite {

  private val legacyHeader = hex"0000802069ed618b1e0f87ce804e2624a34f507c13322c8f28b3010000000000000000000e370896b5a57abf5426bc51e82a3fcb626a5af32c9eb8147fe9c513efbb1701235abe5e397a111728cf439c".toArray
  private val xbtV2Header = hex"000000a01f1e1d1c1b1a191817161514131211100f0e0d0c0b0a0908070605040302010000112233445566778899aabbccddeeff00102030405060708090a0b0c0d0e0f0a8913577ffff001d0df0ad0b44332211efcdab89ffeeddccbbaa998877665544332211005802000003001c000000000000000000000000000000000040d10c008967452301efcdab8967452301efcdab8967452301efcdab8967452301efcdab".toArray

  test("identify and parse historical 80-byte headers") {
    assert(BlockHeaderParser.expectedSize(legacyHeader) == Right(BlockHeaderParser.LegacySerializedSize))

    val parsed = BlockHeaderParser.parse(legacyHeader)

    assert(parsed == Right(BlockHeader.read(legacyHeader)))
    assert(BlockHeader.write(parsed.toOption.get).sameElements(legacyHeader))
  }

  test("identify and parse 164-byte XBT version-2 headers") {
    assert(BlockHeaderParser.expectedSize(xbtV2Header) == Right(BlockHeaderParser.Version2SerializedSize))

    val parsed = BlockHeaderParser.parse(xbtV2Header)

    assert(parsed.isRight)
    assert(BlockHeader.write(parsed.toOption.get).sameElements(xbtV2Header))
  }

  test("reject truncated and trailing header bytes") {
    assert(BlockHeaderParser.expectedSize(Array[Byte](0, 1, 2)).isLeft)
    assert(BlockHeaderParser.parse(legacyHeader.dropRight(1)).isLeft)
    assert(BlockHeaderParser.parse(legacyHeader :+ 0.toByte).isLeft)
    assert(BlockHeaderParser.parse(xbtV2Header.dropRight(1)).isLeft)
    assert(BlockHeaderParser.parse(xbtV2Header :+ 0.toByte).isLeft)
  }
}
