package fr.acinq.eclair.blockchain

import com.ashbornelabs.bitcoin.xbt.{BlockHeader, BlockHeaderV2}

import scala.util.Try

/** Decodes serialized legacy and XBT version-2 block headers. */
object BlockHeaderParser {

  val LegacySerializedSize = 80
  val Version2SerializedSize = 164

  /**
   * Parse exactly one serialized block header.
   *
   * The high bit of the wire version selects the XBT version-2 format. The
   * length is checked before delegating to bitcoin-xbt-kmp so callers cannot
   * accidentally parse a header with trailing or truncated bytes.
   */
  def parse(bytes: Array[Byte]): Either[String, BlockHeader] = {
    if (bytes.length < 4) {
      Left(s"block header is truncated: expected at least 4 bytes, got ${bytes.length}")
    } else {
      val versionOnWire = readUInt32LittleEndian(bytes)
      val expectedSize = if ((versionOnWire & BlockHeaderV2.VERSION_FLAG) != 0) Version2SerializedSize else LegacySerializedSize
      if (bytes.length != expectedSize) {
        Left(s"invalid block header length: expected $expectedSize bytes, got ${bytes.length}")
      } else {
        Try(BlockHeader.read(bytes)).toEither.left.map { error =>
          Option(error.getMessage).getOrElse(error.getClass.getSimpleName)
        }
      }
    }
  }

  private def readUInt32LittleEndian(bytes: Array[Byte]): Long = {
    (bytes(0).toLong & 0xffL) |
      ((bytes(1).toLong & 0xffL) << 8) |
      ((bytes(2).toLong & 0xffL) << 16) |
      ((bytes(3).toLong & 0xffL) << 24)
  }
}
