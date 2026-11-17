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

Pull requests should name the profile version when changing behavior. Issue
#12 tracks Eclair's implementation and conformance coverage.
