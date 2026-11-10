# Bug log - embedded mod review fixes (main @ 6566b9d)

(Numbering keeps the original review labels; BUG-1/ISSUE-1 and BUG-8 were moved to the unresolved section below.)


## BUG-2: Fake Minecraft UUIDs (Mojang API never called)
- **Bug**: `MojangAPIService.getProfile` fabricated a name-hash UUID for any name; `LinkingService.createLink` generated its own name-based UUID, so stored links and Mojang lookups were not real.
- **Cause**: Stub implementation kept after the merge.
- **Repro**: Link any Discord user; stored UUID is a name hash, not the Mojang UUID. A lookup of a nonexistent name still "succeeded".
- **Fix**: Real Mojang profile lookup (`api.mojang.com`, 5s timeout, 5-minute cache); nonexistent names return empty; `createLink` now takes the resolved Mojang UUID.
- **Tested**: Unit compile; runtime lookup path exercised via code review (no live Mojang call in CI).

## BUG-3: Whitelist add/remove confirmed success without a real outcome
- **Bug**: `handleWhitelistAdd` logged success and posted the Discord confirmation before the command ran. Also, `performPrefixedCommand` returns void and consumes vanilla failures (already-whitelisted add, nonexistent remove), so even catching exceptions cannot detect failure.
- **Fix**: Confirmation now sent from inside the server task, and only after verifying the resulting STORED whitelist membership via `UserWhiteList.isWhiteListed(NameAndId)` (stored-list contains). Note `PlayerList.isWhiteListed` is login eligibility - true when the whitelist is disabled or the player is op - and is NOT used. Verification is tri-state: present/absent/unknown. Add succeeds only on confirmed present; remove succeeds only on confirmed absent; a verification error (unknown) counts as failure, never as confirmed absence. Kick behavior unchanged.
- **Tested**: New unit tests for the outcome predicates incl. the unknown-is-failure case (9 tests pass). Bytecode-verified on the resolved 1.21.11 mapped jar: UserWhiteList.isWhiteListed = stored contains; PlayerList.isWhiteListed = isUsingWhitelist()==false || op || member.

## BUG-4: `/discord test` open to everyone and did nothing
- **Bug**: No permission requirement, and the handler only replied - no test message was ever posted despite the reply claiming one.
- **Fix**: Now requires operator level 2 and posts a real test message to the configured log channel; failure is reported when the bot/log channel is not configured.
- **Tested**: Compile.

## BUG-5: `/whitelist tickets` missing staff check
- **Bug**: Any Discord member could post the whitelist request panel (the sibling `log` and `unlink` subcommands already checked staff).
- **Fix**: Added the same `hasStaffPermission` check.
- **Tested**: Compile.

## BUG-6: Approve/deny buttons missing staff check
- **Bug**: `whitelist_approve:`/`whitelist_deny:` buttons executed approve/deny for any user who clicked them.
- **Fix**: Both paths now check `hasStaffPermission` and refuse ephemerally.
- **Tested**: Compile.

## BUG-7: Placeholder discord id "none" corrupted the Discord index
- **Bug**: In-game `/whitelist add` stores links with discord id `none`; every such link overwrote the single `linksByDiscord["none"]` entry, and removing one player's link removed the shared mapping.
- **Fix**: `PlayerLinkingManager` no longer indexes placeholder ids (null/blank/"none") by discord id, on add, remove, and load.
- **Tested**: Added `PlayerLinkingManagerTest.testPlaceholderDiscordIdNotIndexed`.


## BUG-9: Log channel lost on restart; config fields documented but not implemented
- (amended) `/whitelist log` now also reports when persisting to the config file fails, instead of claiming durable success.
- **Bug**: `/whitelist log` kept the channel in memory only; `logChannelId`/`staffRoleId` were documented in config but never read.
- **Fix**: Config gains working `logChannelId`/`staffRoleId`; `/whitelist log` persists the channel; startup restores it; staff role falls back to `DISCORD_STAFF_ROLE_ID`.
- **Tested**: Compile.

## BUG-10: Pending whitelist requests leaked
- **Bug**: `pendingRequests` entries were never removed when a request completed via the confirmation path (only approve/deny removed them).
- **Fix**: The terminal `updateRequestMessage` now consumes the entry.
- **Tested**: Compile.

## Reported but intentionally NOT fixed (awaiting maintainer decision)

### Source layout does not match declared packages
Sources live under `src/.../combat/log/report/*` while declaring `package whitelisting.swordsmp.*`. The Gradle build succeeds (verified on main @ 6566b9d) because explicit file lists are compiled, so this is hygiene, not a build failure. Left untouched per scope ("don't change anything else"); a relocation patch can be produced if wanted.

### Corrupt `player-links.json` handling (policy choice)
`loadLinks()` catches only `IOException`; malformed JSON (e.g. truncated file) throws `JsonSyntaxException` during mod init, and a later save would overwrite the file. No fix shipped: any handling (start fresh, preserve-and-fresh, fail-safe) is a data policy the maintainer should choose. Baseline behavior kept as-is for now.

## BUG-11: Renamed Minecraft account could be silently transferred to another Discord user
- **Bug**: The duplicate check ran by name only, before Mojang resolution. With real UUIDs, a renamed account (OldName -> NewName, same UUID) passed the name check and `addLink` overwrote the UUID with a new Discord owner while the old owner's discord index entry went stale. (Reviewer's artifact-only harness reproduced: ownerA/OldName/UUID overwritten by ownerB/NewName/same UUID.)
- **Fix**: Atomic `addLinkChecked`/`createLinkChecked`: if the resolved UUID is already linked to a different Discord account, the request is rejected ("already linked to a different Discord user") and no state changes. The link is also no longer written before the command outcome - it is created only after the whitelist state is confirmed, so a failed add never blocks a retry. In-game adds never overwrite an existing link for the same UUID. Approve path uses the checked insert too.
- **Tested**: New unit tests `testAddLinkCheckedRejectsUuidOwnedByOtherDiscord` (conflict rejected, original owner intact, no stale index) and `testAddLinkCheckedAllowsSameOwnerRelink` (rename by the same Discord user works). 9 tests pass.

## BUG-12: Deny modal did not recheck staff permission at submission
- **Bug**: Staff permission was checked when opening the deny modal, but the mutating endpoint (`handleDeny`) trusted it. A user who lost the staff role between opening and submitting could still deny.
- **Fix**: `handleDeny` rechecks `hasStaffPermission` before denying. (Path currently dormant - no review message is posted - but the check is now correct for when the flow is enabled.)
- **Tested**: Compile.

## Packaging regression caught in review (never shipped)
- Removing okhttp/okio/kotlin-stdlib from `build.gradle` also removed them from the fat jar, but JDA 5.0.0 requires okhttp 4.12.0 at runtime (its POM says so; `JDABuilder` references okhttp classes). The 1.1.0 jar built without them would throw `NoClassDefFoundError` when the bot starts. Restored and verified: the built jar contains okhttp3/okio/kotlin classes, and an artifact-only harness (built jar as the only classpath entry) loads `JDABuilder`, `okhttp3.OkHttpClient$Builder`, `okio.Buffer` and `kotlin.jvm.internal.Intrinsics` successfully. sqlite-jdbc stays removed (no code uses JDBC).

## Other review follow-ups
- `/discord test` reply now says "Queued a test message..." (queue() is asynchronous; delivery failures are logged via the failure callback).
- CI workflow keeps the `gradle/actions/wrapper-validation@v4` security check.
- Install docs reference the real artifact name `whitelisting-via-discord-*.jar`.

## Cleanup (not bugs, per findings)
- Removed dead code: `ExampleMixin`, unused client entrypoint/mixins, unreachable `whitelist_link_modal` flow and `linkFromVanillaThread` stub, unused 4-arg notification overload, unused `setLogChannelInfo`/`updateWhitelistListMessage`.
- Removed unused sqlite-jdbc dependency from `build.gradle` and the fat-jar list (okhttp/okio/kotlin-stdlib stay: JDA needs them at runtime).
- Deleted tracked runtime artifacts: `whitelist-mod/logs/*`, `whitelist-mod/build.log`; `.gitignore` now covers them.
- Deleted stale `docs/websocket-api.md` (no WebSocket service exists anymore).
- Fixed `fabric.mod.json` metadata (author, homepage/sources), `config.example.json` now matches the real flat config, READMEs/CONFIG_SYSTEM.md corrected to match behavior, CI workflow builds `whitelist-mod/` (it previously ran a nonexistent root `./gradlew`).
- Version bumped to 1.1.0.

## Known product decisions (left as-is, for the maintainer)
- Discord whitelist requests are auto-approved instantly; the approve/deny button flow exists but no review message is posted anywhere, so it is currently unreachable.
