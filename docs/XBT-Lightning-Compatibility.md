# XBT Lightning compatibility profile

Eclair's XBT Lightning behavior follows the project profile
`xbt-lightning-compat-v0.1`. The profile reuses standard Lightning wire formats
and chain identifiers while marking XBT-specific capabilities in feature
vectors. It is a project compatibility contract, not an official BOLT
assignment.

## Feature assignments

| Feature | Mandatory bit | Optional bit | Contexts |
|---|---:|---:|---|
| `option_blake2b` | 512 | 513 | `init`, node announcements, BOLT 11, BOLT 12 |
| `option_unified_sigs` | 514 | 515 | `init`, node announcements, channel type |

`option_unified_sigs` depends on `option_blake2b`. Feature vectors use normal
BOLT bit ordering. An origin must not set both forms of a feature pair.

## Peer and channel behavior

An XBT node advertises mandatory `option_blake2b`. A peer that omits it may
remain connected, but Eclair refuses channel creation in either direction with
that peer. This permits non-channel Lightning clients to connect without
allowing an XBT channel to fall back to legacy behavior.

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
| Remote signature on an anchor-format HTLC second-stage transaction | `SIGHASH_SINGLE | SIGHASH_ANYONECANPAY | SIGHASH_UNIFIED` (`0x83`) |
| Remote signature on a zero-fee commitment HTLC second-stage transaction | `SIGHASH_ALL | SIGHASH_UNIFIED` (`0x21`) |
| SegWit v0 force-close claim spending a channel output | `SIGHASH_ALL | SIGHASH_UNIFIED` (`0x21`) |

The remote HTLC signature differs by commitment format because anchor-format
transactions preserve the existing `SINGLE|ANYONECANPAY` fee-bumping behavior,
while zero-fee commitment transactions use `ALL`. Eclair validates the
received HTLC signatures against the corresponding format-specific value and
does not retry with a legacy sighash. Taproot channel signing remains on its
existing sighash rules and is not negotiated together with unified signatures.

## Wallet transaction signing

When Knots reports BLAKE2b as scheduled, wallet PSBT signing requires the
explicit `SIGHASH_ALL | SIGHASH_UNIFIED` value (`0x21`) for both SegWit v0 and
Taproot key-path inputs. Eclair does not accept legacy sighash metadata as a
fallback in that mode. Unified sighash requires spent-output data for every
transaction input.

## Chain identity and invoices

Lightning `chain_hash` remains the Bitcoin genesis hash for the selected XBT
network. Standard Lightning invoice HRPs remain in use. XBT payment artifacts
carry mandatory `option_blake2b`, so implementations that do not understand
the XBT feature reject them under normal required-feature rules.

## Conformance and implementation scope

Pull requests should name the profile version when changing behavior. Channel
feature parsing, channel-type selection, downgrade rejection, and unified
channel transaction signing are covered by Eclair's conformance tests. Live
interoperability with other XBT Lightning implementations remains a separate
integration check; unit tests do not establish cross-implementation
interoperability.
