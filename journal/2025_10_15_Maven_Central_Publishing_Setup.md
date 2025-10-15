# 2025-10-15: Maven Central Publishing Setup

**Date**: 2025-10-15
**Status**: 🚧 In Progress

## Overview

Setting up Maven Central publishing for the Kotlin Notion Client library (v0.1.0). This session focuses on configuring the build system to publish artifacts to Maven Central, which is the standard repository for professional Kotlin/Java libraries.

**Learning Objective**: Understand the complete Maven Central publishing workflow from setup to release.

## Why Maven Central?

Maven Central is the default repository for the Java/Kotlin ecosystem:
- **Discoverability**: Developers expect to find libraries there
- **Trust**: Established, curated repository with quality standards
- **Ease of Use**: Works out-of-the-box with Gradle/Maven (no additional repository configuration)
- **Professional Standard**: Industry standard for open-source JVM libraries

**Alternative**: JitPack is easier but requires users to add a custom repository and is less discoverable.

## Maven Central Publishing Requirements

### High-Level Overview

Publishing to Maven Central involves:
1. **Account Setup**: Create Sonatype JIRA account and claim your namespace (groupId)
2. **Build Configuration**: Configure Gradle to generate proper artifacts
3. **POM Requirements**: Provide required metadata (name, description, URL, license, developers, SCM)
4. **Artifact Signing**: Sign artifacts with GPG to prove authenticity
5. **Publishing**: Upload signed artifacts to Sonatype OSSRH (staging repository)
6. **Release**: Promote artifacts from staging to Maven Central

### Key Concepts

- **Group ID**: Reverse domain identifier (e.g., `it.saabel`)
- **Artifact ID**: Library name (e.g., `kotlin-notion-client`)
- **Version**: SemVer version (e.g., `0.1.0`)
- **Coordinates**: Full identifier: `it.saabel:kotlin-notion-client:0.1.0`
- **POM**: Project Object Model - XML file with project metadata
- **GPG**: GNU Privacy Guard - used to sign artifacts cryptographically
- **OSSRH**: Open Source Software Repository Hosting - Sonatype's staging area
- **Staging**: Temporary holding area where artifacts are validated before release

## Learning Resources

### Official Documentation
- [Sonatype OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Gradle Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Gradle Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html)

### Helpful Tutorials
- [Publishing Kotlin Libraries to Maven Central](https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/)
- [Complete Guide to Publishing a Kotlin Library](https://dev.to/kotlin/how-to-build-and-publish-a-kotlin-library-516p)

### Reference Implementations
We have two Kotlin Notion clients in `/reference/kotlin/` that may have Maven publishing configurations we can learn from.

## Prerequisites Checklist

Before we can publish, we need:
- [ ] **Sonatype JIRA Account** - For namespace claim and OSSRH access
- [ ] **Namespace Verified** - Prove ownership of `it.saabel` groupId
- [ ] **GPG Key Pair** - For signing artifacts
- [ ] **GPG Key Published** - Public key uploaded to key server
- [ ] **GitHub Repository Public** - SCM URL must be accessible
- [ ] **Build Configuration** - Gradle publishing plugins configured
- [ ] **Credentials Configured** - Sonatype username/password in Gradle properties

## Implementation Plan

### Step 1: Research Phase (Current)
**Goal**: Understand the full publishing workflow and gather requirements

**Tasks**:
- [x] Document Maven Central publishing overview
- [ ] Review reference Kotlin implementations for patterns
- [ ] Identify all required metadata fields
- [ ] Create step-by-step checklist

### Step 2: Local Build Configuration
**Goal**: Configure Gradle to generate publishable artifacts (without credentials yet)

**Tasks**:
- [ ] Add `maven-publish` plugin to build.gradle.kts
- [ ] Add `signing` plugin to build.gradle.kts
- [ ] Configure publication with POM metadata
- [ ] Configure sources and javadoc JARs
- [ ] Test local publishing to `mavenLocal()`

### Step 3: GPG Setup
**Goal**: Generate and configure GPG key for artifact signing

**Tasks**:
- [ ] Generate GPG key pair (or use existing)
- [ ] Export public key to key server
- [ ] Configure Gradle with GPG credentials
- [ ] Test signing artifacts locally

### Step 4: Sonatype Account Setup
**Goal**: Claim namespace and get OSSRH credentials

**Tasks**:
- [ ] Create Sonatype JIRA account
- [ ] Create JIRA ticket for `it.saabel` namespace
- [ ] Verify ownership (GitHub repository or DNS TXT record)
- [ ] Receive approval and credentials
- [ ] Configure credentials in `~/.gradle/gradle.properties`

### Step 5: Publishing Configuration
**Goal**: Configure Gradle to publish to Sonatype OSSRH

**Tasks**:
- [ ] Add Sonatype repository URLs to publishing configuration
- [ ] Configure signing for all publications
- [ ] Test publishing to OSSRH staging
- [ ] Verify artifacts in staging repository

### Step 6: Release Process
**Goal**: Promote artifacts from staging to Maven Central

**Tasks**:
- [ ] Review staged artifacts
- [ ] Close staging repository (triggers validation)
- [ ] Release staging repository (promotes to Central)
- [ ] Verify artifacts appear on Maven Central
- [ ] Test installation from Maven Central

## Work Log

### Session 1: Research, Configuration & Local Testing ✅ COMPLETE
**Date**: 2025-10-15
**Duration**: ~2 hours
**Status**: Build configuration complete, local publishing verified

**Completed**:
1. ✅ Created journal entry with learning-focused structure
2. ✅ Researched Maven Central requirements and best practices
3. ✅ Studied GPG keyserver ecosystem and security practices
4. ✅ Reviewed reference implementations for patterns
5. ✅ Added Dokka plugin to libs.versions.toml
6. ✅ Configured Maven publishing in build.gradle.kts:
   - Added `maven-publish` and `signing` plugins
   - Set JVM toolchain to 17 (modern LTS, wide compatibility)
   - Configured sources and javadoc JARs
   - Added complete POM metadata (name, description, licenses, developers, SCM)
   - Configured Sonatype OSSRH repository URLs
   - Set up signing configuration (conditional - only for Maven Central publishes)
7. ✅ Fixed JVM target compatibility issue (JVM 17 toolchain)
8. ✅ Resolved Dokka V1 compatibility issue (empty javadoc JAR for now)
9. ✅ Successfully built project
10. ✅ Published to mavenLocal() and verified artifacts

**Artifacts Generated** (`~/.m2/repository/it/saabel/kotlin-notion-client/0.1.0/`):
- `kotlin-notion-client-0.1.0.jar` (2.5MB) - Main library
- `kotlin-notion-client-0.1.0-sources.jar` (113KB) - Source code
- `kotlin-notion-client-0.1.0-javadoc.jar` (261B) - Empty javadoc (Maven Central compliant)
- `kotlin-notion-client-0.1.0.pom` (4KB) - POM with complete metadata
- `kotlin-notion-client-0.1.0.module` (5.8KB) - Gradle metadata

**POM Verification**: ✅
- Group ID: it.saabel
- Artifact ID: kotlin-notion-client
- Version: 0.1.0
- Name, description, URL: Present
- MIT License: Configured
- Developer info (Jonas Saabel): Present
- SCM (GitHub): Configured
- Issue management: Configured
- Dependencies: All correctly listed

**Decisions Made**:
1. **JVM Target**: 17 (LTS, widely compatible, modern features)
2. **Publishing Approach**: Manual configuration (better for learning vs Vanniktech plugin)
3. **Dokka**: Postponed V2 migration, using empty javadoc JAR (acceptable for Maven Central)
4. **Signing**: Conditional (only when publishing to Maven Central, not for local builds)

**Known Issues**:
- Dokka V1 deprecated, needs migration to V2 (non-blocking)
- Dokka incompatible with JDK 25 (workaround: empty javadoc JAR)
- GPG key upload blocked by corporate firewall (will retry from home network)

**Learning Outcomes**:
- Understood Maven Central requirements (POM metadata, JARs, signing)
- Learned about JVM toolchain vs local JDK
- Explored GPG keyserver ecosystem and deprecation of SKS
- Reviewed security best practices for key management
- Understood library compatibility considerations (target JVM version)

### Next Steps (To Complete Before Publishing)

**Step 1: Upload GPG Key to Keyserver** (Blocked by corporate firewall)
- Try from home network or mobile hotspot
- Recommended keyserver: `keyserver.ubuntu.com`
- Backup keyserver: `keys.openpgp.org`
- Command: `gpg --keyserver hkps://keyserver.ubuntu.com --send-keys YOUR_KEY_ID`
- Verify upload: `gpg --keyserver hkps://keyserver.ubuntu.com --search-keys YOUR_EMAIL`

**Step 2: Configure GPG Signing Locally** ✅
Create `~/.gradle/gradle.properties` with:
```properties
# Maven Central Portal credentials (from user token generation)
mavenCentralUsername=<token-username>
mavenCentralPassword=<user-token-password>

# GPG signing (traditional secring.gpg approach)
signing.keyId=<short-8-char-key-id>
signing.password=<gpg-key-passphrase>
signing.secretKeyRingFile=/Users/yourusername/.gnupg/secring.gpg
```

**Key Points**:
- **Maven Central credentials**: Use the username/password from the user token you generate in Maven Central Portal (NOT your login credentials)
- **signing.keyId**: Use the **short** 8-character key ID (last 8 chars of the long key ID)
  - Get it with: `gpg --list-secret-keys --keyid-format SHORT`
  - Example: If long ID is `8C67477498043F87`, short ID is `98043F87`
- **secring.gpg file**: Can be regenerated anytime using:
  ```bash
  gpg --batch --no-tty --pinentry-mode loopback --passphrase "YOUR_PASSPHRASE" \
      --export-secret-keys <YOUR_SHORT_KEY_ID> > ~/.gnupg/secring.gpg
  ```
  - As long as you have your GPG key and passphrase, you can always regenerate this file
  - The secring.gpg file is essentially an export of your secret key in a format Gradle expects

**Step 3: Create Maven Central Account**
1. Go to: https://central.sonatype.com/
2. Sign up for account
3. Create namespace verification ticket for `it.saabel`
4. Verify ownership via:
   - GitHub repository: Create `jsaabel/it.saabel` repo, OR
   - DNS TXT record for `saabel.it`
5. Wait for approval (usually 1-2 business days)
6. Generate user token for publishing

**Step 4: Test Publishing with Signing**
Once credentials are configured:
```bash
# Test signing locally
./gradlew publishToMavenLocal

# Verify .asc signature files are created
ls ~/.m2/repository/it/saabel/kotlin-notion-client/0.1.0/*.asc
```

**Step 5: Publish to Maven Central Staging**
```bash
# Publish to staging repository
./gradlew publishMavenPublicationToMavenCentralRepository

# Login to Sonatype Nexus to review
# https://s01.oss.sonatype.org/
```

**Step 6: Release to Maven Central**
1. Login to Sonatype Nexus
2. Find your staging repository
3. "Close" repository (triggers validation)
4. If validation passes, "Release" repository
5. Wait 10-30 minutes for sync to Maven Central
6. Verify at: https://repo1.maven.org/maven2/it/saabel/kotlin-notion-client/

---

## Session 2: Successful Upload to Maven Central Portal ✅
**Date**: 2025-10-15
**Status**: Artifacts uploaded to Portal, ready for final publish

**Completed**:
1. ✅ Successfully resolved GPG signing configuration issues
2. ✅ Generated `secring.gpg` file using batch export with passphrase
3. ✅ Configured `~/.gradle/gradle.properties` with all required credentials
4. ✅ Verified local signing works (all `.asc` files generated correctly)
5. ✅ Published artifacts to Maven Central Portal using `./gradlew publishToMavenCentral`
6. ✅ Verified artifacts in Portal deployment interface

**Current Status**: Artifacts are uploaded and ready for final "Publish" action in the Portal.

### Pre-Release Checklist (Before Clicking "Publish")

**⚠️ IMPORTANT: Publishing to Maven Central is PERMANENT and IMMUTABLE**
- Cannot delete versions
- Cannot modify artifacts
- Cannot change metadata
- Can only publish newer versions

#### 1. Local Integration Test 🧪
- [ ] **Publish to Maven Local**: Run `./gradlew publishToMavenLocal` one final time
- [ ] **Create Test Project**: Create a fresh Gradle project in a separate directory
- [ ] **Add Dependency**:
  ```kotlin
  repositories {
      mavenLocal()
  }
  dependencies {
      implementation("it.saabel:kotlin-notion-client:0.1.0")
  }
  ```
- [ ] **Test Basic Functionality**: Write a simple test that uses key library features
- [ ] **Verify All Artifacts Work**: Ensure sources and javadoc are accessible in IDE
- [ ] **Test Import Statements**: Verify package structure makes sense for users
- [ ] **Check Transitive Dependencies**: Ensure all dependencies resolve correctly

#### 2. Repository & Documentation Review 🔍
- [ ] **README.md Review**: Verify installation instructions, usage examples, API overview
- [ ] **Documentation Completeness**: Ensure guides are clear and helpful for new users
- [ ] **Code Examples**: Test that all code examples in docs actually work
- [ ] **API Documentation**: Review inline KDoc comments
- [ ] **CHANGELOG.md**: Consider documenting what's included in v0.1.0

#### 3. GitHub Repository Preparation 📦
- [ ] **Make Repository Public**: Currently private, must be public for Maven Central
  - POM references `https://github.com/jsaabel/kotlin-notion-client`
  - Users will expect to access this URL
- [ ] **GitHub Profile Update**: Update profile if desired before making repo public
- [ ] **Repository Description**: Add clear, concise description
- [ ] **Topics/Tags**: Add relevant topics (kotlin, notion, api-client, etc.)
- [ ] **GitHub License Display**: Ensure LICENSE file is recognized
- [ ] **Repository Social Preview**: Consider adding a social preview image

#### 4. Multi-Machine Publishing Setup 🖥️
- [ ] **Transfer GPG Key to Personal Machine**:
  ```bash
  # Export on work machine (armor format for text transfer)
  gpg --export-secret-keys --armor <KEY_ID> > private-key.asc

  # Import on personal machine
  gpg --import private-key.asc

  # Generate secring.gpg on personal machine
  gpg --batch --no-tty --pinentry-mode loopback --passphrase "YOUR_PASSPHRASE" \
      --export-secret-keys <SHORT_KEY_ID> > ~/.gnupg/secring.gpg

  # Securely delete transfer file
  shred -u private-key.asc  # Linux
  # or
  rm -P private-key.asc     # macOS
  ```
- [ ] **Copy Gradle Properties**: Transfer `~/.gradle/gradle.properties` securely
- [ ] **Test Signing on Personal Machine**: Run `./gradlew publishToMavenLocal` and verify `.asc` files
- [ ] **Document Publishing Process**: Ensure you can publish from either machine

#### 5. Artifact Verification in Portal ✅
- [ ] **All Required Files Present**:
  - [ ] `kotlin-notion-client-0.1.0.jar` (main library)
  - [ ] `kotlin-notion-client-0.1.0-sources.jar`
  - [ ] `kotlin-notion-client-0.1.0-javadoc.jar`
  - [ ] `kotlin-notion-client-0.1.0.pom`
  - [ ] All corresponding `.asc` signature files
- [ ] **POM Metadata Correct**:
  - [ ] Name, description, URL
  - [ ] License (MIT)
  - [ ] Developer information
  - [ ] SCM URLs (GitHub)
  - [ ] Dependencies and versions
- [ ] **File Sizes Reasonable**: No unexpectedly large/small files

#### 6. Code Quality Final Check 🔬
- [ ] **All Tests Passing**: Run `./gradlew test`
- [ ] **Linting Clean**: Run `./gradlew formatKotlin lintKotlin`
- [ ] **Build Successful**: Run `./gradlew build`
- [ ] **No Secrets in Code**: Double-check for API keys, tokens, credentials
- [ ] **No Debug Code**: Remove println statements, commented code, TODOs in critical paths

#### 7. Version & Stability Considerations 🏷️
- [ ] **Version 0.1.0 Appropriate**: Signals early/experimental release
- [ ] **API Surface Acceptable**: Comfortable with these public APIs for now
- [ ] **Breaking Changes Expected**: Understand that 0.x.x releases may have breaking changes
- [ ] **Migration Path**: Consider future versioning strategy (when to reach 1.0.0)

#### 8. Post-Publication Plan 📋
- [ ] **Verification Strategy**: How will you verify it's accessible on Maven Central?
- [ ] **Test Installation**: Plan to create a fresh project and install from Maven Central
- [ ] **Announcement Plan**: Where will you announce the release? (if anywhere)
- [ ] **Issue Tracking**: GitHub Issues enabled and ready for feedback
- [ ] **Support Plan**: How will you handle bug reports and feature requests?

### Recommendations Before Publishing

**Priority 1 - MUST DO** (Blocking):
1. Publish to `mavenLocal()` and test in a separate project
2. Review and update README.md with clear installation/usage instructions
3. Make GitHub repository public
4. Set up GPG key on personal machine and test signing

**Priority 2 - SHOULD DO** (Strongly recommended):
1. Run full test suite one more time
2. Add repository description and topics on GitHub
3. Verify all checklist items above

**Priority 3 - NICE TO HAVE** (Non-blocking):
1. Update GitHub profile
2. Add social preview image
3. Create CHANGELOG.md

**Priority 4 - CAN DO AFTER** (Post-publication):
1. Announce release
2. Write blog post or tutorial
3. Submit to Kotlin Weekly
4. Create demo project

### Key Decision Point

**Should we publish now or address the checklist items first?**

Given that:
- This is v0.1.0 (signals experimental/early release)
- Publishing is permanent and immutable
- GitHub repo must be public (POM references it)
- Documentation should be ready for first users
- Should verify artifacts work correctly via local integration test

**Recommendation**: Address Priority 1 items before publishing. This ensures:
- A good first impression with proper documentation
- Functional publishing workflow from both machines
- Confidence that the artifacts actually work as intended

---

## Notes & Observations

- Maven Central publishing is more involved than JitPack but provides better discoverability
- The process has clear steps: Build Config → GPG → Sonatype Account → Publish → Release
- Most of the complexity is one-time setup; subsequent releases are straightforward
- Can test everything locally before needing Sonatype credentials

## Questions to Explore

- [ ] Do reference implementations use any helpful Gradle plugins for publishing?
- [ ] What's the typical turnaround time for namespace verification?
- [ ] Can we automate signing without exposing private keys?
- [ ] Should we set up GitHub Actions for automated publishing in the future?

## Success Criteria

**For This Session**:
- [ ] Understand complete Maven Central publishing workflow
- [ ] Configure local publishing (to mavenLocal())
- [ ] Generate and configure GPG key
- [ ] Create Sonatype JIRA account and ticket
- [ ] Document clear instructions for future releases

**For Final Release**:
- [ ] Library available at: `https://repo1.maven.org/maven2/it/saabel/kotlin-notion-client/0.1.0/`
- [ ] Can install with: `implementation("it.saabel:kotlin-notion-client:0.1.0")`
- [ ] No custom repository configuration needed
- [ ] Artifacts properly signed and verified