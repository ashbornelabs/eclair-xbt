# XBT Knots Regtest testing

Eclair's live blockchain integration tests use XBT Knots Regtest. The tests must
not use Bitcoin Core or a Bitcoin test network as a substitute: the node's
consensus rules, block headers, and wallet behavior are part of the test target.

## Pinned node

The default test binary is pinned to the Knots release below. The source
commit records the exact source revision for reproducible local builds:

| Item | Value |
|---|---|
| Release tag | `v29.4.3.knots20260508` |
| Source commit for local builds | `a7c4617524c334f542484bfef3919b9f12573840` |
| Linux x86_64 archive SHA-256 | `4c67f1139b668fc3b98e339dd16112c636b2c0f42c2294b98fb92d1d27943e24` |
| macOS x86_64 archive SHA-256 | `43fc171bba9b54944b69790ea77bb81b4214ec1f8b54862dd927a380fbcb9d57` |
| Windows x86_64 archive SHA-256 | `3035ee70d108f6ff7a76b99df9e261181da34a471b30946cf5a1e5e504ec709` |

The release URLs and archive checksums are configured in `eclair-core/pom.xml`.
When Maven runs the test-resource generation phase, it downloads the platform
archive, verifies its SHA-256, and unpacks it under `eclair-core/target`. The
integration fixture uses that `bitcoind` binary by default. The fixture then
checks the node's Knots identity and advertised consensus parameters before
starting Eclair.

On Linux, the pinned source revision can also be built locally. Build Knots
with ZMQ enabled, then point the tests to the directory containing the
resulting `bitcoind` executable:

```bash
git clone https://github.com/bitcoinknots/bitcoin.git
cd bitcoin
git checkout --detach a7c4617524c334f542484bfef3919b9f12573840
cmake -B build -DWITH_ZMQ=ON -DBUILD_SHARED_LIBS=OFF -DBUILD_TESTS=OFF -DBUILD_BENCH=OFF -DENABLE_IPC=OFF
cmake --build build -j"$(nproc)"
cd ..
BITCOIND_DIR="$PWD/bitcoin/build/bin" ./mvnw test
```

`BITCOIND_DIR` must name a directory containing `bitcoind`; it does not name
the executable itself. Without that variable, the fixture looks for the
downloaded release under the Eclair core module's build directory.

## Issue #11 coverage

These suites exercise the live Knots fixture or its test-only activation
variant:

| Suite | Coverage |
|---|---|
| `StartupIntegrationSpec` | Starts Eclair against Knots and checks the XBT Regtest Lightning chain identity. |
| `ChannelIntegrationSpec` | Opens and confirms channels, exercises channel lifecycle, force close, and on-chain HTLC handling. |
| `PaymentIntegrationSpec` | Exercises direct, routed, multipart, trampoline, and blinded payments over channels funded on Knots. |
| `MessageIntegrationSpec` | Exercises channel opening and cooperative close using Knots transactions and confirmations. |
| `ZmqWatcherSpec` | Exercises Knots ZMQ block and transaction notifications, including confirmation updates across a Regtest reorg and ZMQ reconnection. |
| `ActivatedKnotsZmqWatcherSpec` | Uses a test-only BLAKE2b activation height to check legacy and activation headers and reorg confirmation handling. |
| `KnotsRestartRecoveryIntegrationSpec` | Restarts Eclair with its existing datadir after mining a channel, verifies channel and node identity recovery, reconnects to its peer, and checks chain-height catch-up. |
| `BitcoinCoreClientSpec` | Exercises Knots wallet funding, transaction replacement, and package-related RPC paths; this does not alone prove channel-level fee-bump integration. |
| `BitcoinCoreClientWithEclairSignerSpec` | Exercises wallet transactions signed by Eclair and Knots RPC acceptance with the test activation schedule. |

Run the Maven test suite from the repository root to execute the checked-in
ScalaTest suites:

```bash
./mvnw test
```

This command may take several minutes because many integration suites start a
fresh Knots Regtest process and exercise real RPC, wallet, ZMQ, and channel
flows. The suite list above describes source coverage; it does not by itself
record that a particular run passed. Record the exact branch/revision, command,
exit status, and relevant failing suite output when reporting validation.

## Diagnosing failures

Start by checking the logged `using bitcoind` path and the Knots version
reported by `getnetworkinfo`. The fixture rejects a node whose Knots marker or
Regtest consensus profile does not match expectations. Also check that the
selected RPC and ZMQ ports are available and that the Knots process and test
wallet started successfully.

Use the failing boundary to localize the problem:

| Symptom | First checks |
|---|---|
| Knots profile validation fails | Confirm the binary path, release tag or source commit, `getnetworkinfo`, and `getdeploymentinfo` output. |
| Eclair startup or chain identity fails | Check Eclair's selected XBT network profile, configured RPC/ZMQ endpoints, RPC logs, and Knots startup output. |
| Header parsing, activation, or reorg handling fails | Compare the raw Knots block/header response with the XBT header decoder and Eclair's watcher logs; identify whether the mismatch begins in Knots, `bitcoin-xbt-kmp`/`bitcoin-xbt-lib`, or Eclair ingestion. |
| Wallet signing or transaction acceptance fails | Preserve the unsigned PSBT, signed transaction, Knots `testmempoolaccept`/broadcast response, and signer logs; check the Scala adapter, library signing implementation, and Knots validation separately. |
| Channel or payment flow times out | Check both Eclair node logs, the funding transaction and confirmation height from Knots RPC, peer/channel state events, and mempool contents before attributing the failure to a specific layer. |

Do not report a failure as an Eclair defect until the exact Knots binary and
RPC response are confirmed. Likewise, passing a unit test for a decoder or
signer does not replace the corresponding live Knots integration test.
