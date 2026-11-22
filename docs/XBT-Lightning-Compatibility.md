# XBT Lightning compatibility

Eclair's XBT Lightning behavior follows the XBT Lightning specifications in
the [lightning-blake2b/bolts repository](https://github.com/lightning-blake2b/bolts).
Those specifications are the protocol authority. Eclair-specific notes below
describe implementation behavior and do not override the BOLTs.

## Feature assignments

| Feature | Mandatory bit | Optional bit | Contexts |
|---|---:|---:|---|
| `option_blake2b` | 512 | 513 | `init`, node announcements, BOLT 11, BOLT 12 |
| `option_unified_sigs` | 514 | 515 | `init`, node announcements, channel type |

`option_unified_sigs` depends on `option_blake2b`. Feature vectors use normal
BOLT bit ordering. When both bits of a feature pair are set, implementations
interpret the feature as mandatory.

## Peer and channel behavior

An XBT node advertises mandatory `option_blake2b` in the contexts required by
the XBT BOLTs. This marker identifies XBT support in payment artifacts; it is
not a peer-wide channel-open prerequisite. Channel negotiation follows the
channel-type requirements below, including `option_unified_sigs` when the
selected channel type requires it.

Channel announcements whose short-channel-id block height is below the
selected Knots profile's BLAKE2b activation height are excluded from routing.
The same filter is applied to channels loaded into the routing graph at
startup.

When the selected Knots profile configures a finite BLAKE2b activation height,
channel negotiation requires both peers to advertise `option_unified_sigs` and
the negotiated `channel_type` to include its mandatory bit. Eclair rejects
outgoing and incoming channel opens that would use a legacy channel type. When
the profile disables BLAKE2b activation (`Int.MaxValue`), Eclair may negotiate
the corresponding legacy SegWit v0 channel type with a peer that does not
support unified signatures. Unified signatures are currently supported for the
SegWit v0 anchor and zero-fee commitment channel types; simple taproot channel
types cannot be combined with `option_unified_sigs`.

For public channels, Eclair selects a unified-signature channel type when both
peers advertise support. If unified signatures are required by the selected
Knots profile, channel creation is rejected if that negotiation cannot be made.

### Channel transaction sighashes

Unified-signature channel types use these sighash values:

| Transaction signature | Sighash |
|---|---|
| Funding input and commitment transaction | `SIGHASH_ALL | SIGHASH_UNIFIED` (`0x21`) |
| Local signature on an HTLC second-stage transaction | `SIGHASH_ALL | SIGHASH_UNIFIED` (`0x21`) |
| Remote signature on an anchor-format HTLC second-stage transaction | `SIGHASH_SINGLE | SIGHASH_ANYONECANPAY | SIGHASH_UNIFIED` (`0xa3`) |
| Remote signature on a zero-fee commitment HTLC second-stage transaction | `SIGHASH_SINGLE | SIGHASH_ANYONECANPAY | SIGHASH_UNIFIED` (`0xa3`) |
| SegWit v0 force-close claim spending a channel output | `SIGHASH_ALL | SIGHASH_UNIFIED` (`0x21`) |

For both anchor and zero-fee commitment formats, the peer-supplied signature
on an HTLC second-stage transaction uses `SINGLE|ANYONECANPAY|UNIFIED` (`0xa3`);
the local signature uses `ALL|UNIFIED` (`0x21`). Eclair validates received
signatures against the sighash required by the XBT BOLTs and does not retry
with a legacy sighash. Taproot channel signing remains on its existing sighash
rules and is not negotiated together with unified signatures.

## Wallet transaction signing

When Knots reports BLAKE2b as scheduled, wallet PSBT signing requires the
explicit `SIGHASH_ALL | SIGHASH_UNIFIED` value (`0x21`) for both SegWit v0 and
Taproot key-path inputs. Eclair does not accept legacy sighash metadata as a
fallback in that mode. Unified sighash requires spent-output data for every
transaction input.

## Chain identity and invoices

Lightning `chain_hash` identifies the selected XBT network as specified by the
XBT BOLTs. Standard Lightning invoice HRPs remain in use. XBT BOLT 11 payment
invoices and BOLT 12 offers and invoices carry mandatory `option_blake2b`;
Eclair includes and verifies that marker when generating or accepting these
payment artifacts.

## Conformance and implementation scope

Changes to protocol behavior should cite the relevant XBT BOLT section. Eclair
tests cover feature parsing, channel-type negotiation, payment-artifact
feature requirements, and unified channel transaction signing. Live
interoperability with other XBT Lightning implementations remains a separate
integration check; unit tests do not establish cross-implementation
interoperability.
