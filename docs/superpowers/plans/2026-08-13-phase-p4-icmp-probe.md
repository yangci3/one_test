# Phase P4 ICMP Probe Dual-Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add configurable mock vs real ICMP probe reachability while keeping existing scheduler, start/stop, history, and frontend poll behavior.

**Architecture:** Introduce `SceneProbeReachability` with Mock and Icmp implementations. `SceneProbeServiceImpl.tick()` delegates reachability decisions; icmp mode tracks consecutive fail/success counts in memory and updates status + history only on actual transitions.

**Tech Stack:** Spring Boot `@Value` config, `ProcessBuilder` Windows `ping`, existing MyBatis probe state/event mappers

**Spec:** `docs/superpowers/specs/2026-08-13-phase-p4-icmp-probe-design.md`

## Global Constraints

- Default `scene.probe.mode=mock`; switch via yml + backend restart
- ICMP: consecutive fails >= 3 -> offline; consecutive successes >= 1 -> online (config keys)
- Empty/invalid IP: skip ping; do not change status this tick
- History only on real status change (existing `recordEvent` + trim)
- Windows ping: `ping -n 1 -w <timeoutMs> <ip>`; prefer exit code
- Same tick: **serial** ping of monitoring devices (YAGNI; avoid process storm)
- No admin UI for mode; no SNMP; no P3 tiles
- Docs UTF-8 BOM; Git: `D:\gitgit\Git\bin\git.exe`
- Branch: create `feature/phase-p4-icmp-probe` from current HEAD
- Frontend probe APIs unchanged

---

## File Structure

| Path | Role |
| --- | --- |
| `ruoyi-admin/.../application.yml` | Add `mode`, `icmp.*` keys |
| `.../probe/SceneProbeReachability.java` | Interface: `boolean isReachable(String ip)` or result enum |
| `.../probe/MockProbeReachability.java` | Probability flip helper used by tick (or returns desired next status) |
| `.../probe/IcmpProbeReachability.java` | ProcessBuilder ping wrapper |
| `.../probe/ProbeIpUtils.java` | Validate IPv4 (simple) |
| `.../impl/SceneProbeServiceImpl.java` | Wire mode; icmp counters; keep start/stop/history |
| Spec status update | after hand-test |

**Interface decision (lock):** Prefer a small API that fits both modes without awkward mocks:

```java
public interface SceneProbeReachability {
    /** @return true=up, false=down; caller skips if IP invalid before calling icmp */
    boolean probe(String ip);
}
```

Mock mode does **not** call `probe(ip)` for probability ? instead `SceneProbeServiceImpl` keeps mock branch in `tick()` calling private methods moved from current flip logic **OR** Mock implementation exposes `suggestFlip(currentStatus, random)` ? simpler: **keep mock flip logic inside service when mode=mock**; icmp path calls `IcmpProbeReachability.probe(ip)`. Still extract ICMP to its own class. Spec said both strategies ? minimal: interface with one icmp impl + mock path inlined is OK if Mock class holds the probability methods.

**Preferred:** 
- `SceneProbeReachability` with `ReachabilityResult probe(SceneProbeState state, SceneDevice device)` returning SKIP / UP / DOWN
- Mock ignores IP, uses probs + current status
- Icmp SKIP if bad IP; else UP/DOWN from ping

---

### Task 1: Config keys

**Files:**
- Modify: `ruoyi-admin/src/main/resources/application.yml` (scene.probe section)

**Interfaces:**
- Produces yml:
  - `mode: mock`
  - `icmp.timeout-ms: 2000`
  - `icmp.fail-threshold: 3`
  - `icmp.recover-threshold: 1`

- [ ] **Step 1: Add keys under existing `scene.probe` without removing interval/offline/recover probs.**

- [ ] **Step 2: Commit** `Add scene.probe mode and icmp config defaults.`

---

### Task 2: IP utils + ICMP reachability

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/probe/ProbeIpUtils.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/probe/IcmpProbeReachability.java`

**Interfaces:**
- `ProbeIpUtils.isValidIpv4(String ip): boolean` ? trim; reject null/blank; basic dotted-quad
- `IcmpProbeReachability.probe(String ip, int timeoutMs): boolean` ? Windows `ping -n 1 -w timeoutMs ip`; return true iff exit code 0; destroy process on timeout overrun; never throw to tick (catch -> false)

- [ ] **Step 1: Implement utils + ICMP class (serial, no thread pool).**

- [ ] **Step 2: Manual sanity optional: run one ping to 127.0.0.1 from a tiny main or log in test.** If no JUnit infra, skip automated test; document hand check.

- [ ] **Step 3: Commit** `Add ICMP ping reachability helper for scene probe.`

---

### Task 3: Reachability strategy + refactor tick

**Files:**
- Create: `.../probe/SceneProbeReachability.java` (interface)
- Create: `.../probe/MockProbeReachability.java` (probability flip: input current status + probs -> Optional next status or UP/DOWN/SKIP model)
- Modify: `SceneProbeServiceImpl.java`

**Interfaces:**
- Inject `@Value("${scene.probe.mode:mock}") String probeMode`
- Inject icmp timeout / failThreshold / recoverThreshold
- Inject `IcmpProbeReachability` (or construct)
- `ConcurrentHashMap<String, int[]>` or two maps: failCount, successCount by deviceId; clear entry on stop
- `tick()`:
  - if mode invalid -> log warn once, treat as mock
  - if mock: existing probability loop (optionally via MockProbeReachability)
  - if icmp: for each monitoring active state, load device IP; if invalid SKIP; else probe; update counters; maybe set status + recordEvent
- On `stop`/`stopAll`: clear counters for device(s)
- start behavior unchanged (online + event)

- [ ] **Step 1: Implement strategy wiring and icmp state machine per spec.**

- [ ] **Step 2: Compile** `mvn -pl ruoyi-system -am compile -DskipTests` from repo root (if pom present).

- [ ] **Step 3: Commit** `Wire mock/icmp probe modes into SceneProbeServiceImpl tick.`

---

### Task 4: Hand-test notes + mark spec

**Files:**
- Modify: `docs/superpowers/specs/2026-08-13-phase-p4-icmp-probe-design.md` status (UTF-8 BOM)

**Checklist:**
1. mock mode still flips
2. mode=icmp, device IP 8.8.8.8 -> online
3. bad IP -> offline after ~3 intervals
4. empty IP -> no flip spam
5. back to mock works

- [ ] **Step 1: Hand-test or document pending human.**

- [ ] **Step 2: Update spec status; commit** `Mark P4 ICMP probe spec implemented.` (or code-complete pending hand-test)

---

## Self-Review (plan vs spec)

| Spec item | Task |
| --- | --- |
| mode mock/icmp | T1, T3 |
| fail 3 / recover 1 | T3 |
| invalid IP skip | T2, T3 |
| Windows ping | T2 |
| history on change | T3 (existing recordEvent) |
| no P3/SNMP/UI mode | Global Constraints |

---

## Execution Handoff

Plan saved to `docs/superpowers/plans/2026-08-13-phase-p4-icmp-probe.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** ? fresh subagent per task + review
2. **Inline Execution** ? this session with executing-plans checkpoints

Which approach?
