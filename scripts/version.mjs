#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const [command, level = "patch"] = process.argv.slice(2);
const versionFiles = ["package.json", "app/build.gradle.kts"];

if (!["bump", "check"].includes(command)) {
  fail("Usage: node scripts/version.mjs bump <major|minor|patch> OR node scripts/version.mjs check");
}

if (command === "bump") {
  if (!["major", "minor", "patch"].includes(level)) {
    fail("Version bump must be major, minor, or patch.");
  }
  const pkg = readJson("package.json");
  const next = bump(pkg.version, level);
  pkg.version = next;
  writeJson("package.json", pkg);
  updatePackageLock(next);
  replace("app/build.gradle.kts", /versionName = "\d+\.\d+\.\d+"/, `versionName = "${next}"`);
  appendChangelog(next, level);
  console.log(`Version bumped to ${next}.`);
  process.exit(0);
}

const pkgVersion = readJson("package.json").version;
if (fs.existsSync(path.join(root, "package-lock.json"))) {
  const lock = readJson("package-lock.json");
  if (lock.version !== pkgVersion || lock.packages?.[""]?.version !== pkgVersion) {
    fail(`package-lock.json version must match package.json ${pkgVersion}.`);
  }
}
const appBuild = read("app/build.gradle.kts");
if (!appBuild.includes(`versionName = "${pkgVersion}"`)) {
  fail(`app/build.gradle.kts versionName must match package.json ${pkgVersion}.`);
}

if (!read("CHANGELOG.md").includes(`## ${pkgVersion} - `)) {
  fail(`CHANGELOG.md must include an entry for ${pkgVersion}.`);
}

if (hasHead()) {
  const changed = git(["status", "--porcelain=v1"])
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => line.slice(3))
    .filter((file) => !versionFiles.includes(file) && file !== "CHANGELOG.md");
  if (changed.length > 0) {
    const previous = git(["show", "HEAD:package.json"]);
    const previousVersion = JSON.parse(previous).version;
    if (compare(pkgVersion, previousVersion) <= 0) {
      fail(`Changed files require package.json to be bumped above ${previousVersion}. Current ${pkgVersion}.`);
    }
  }
}

console.log("Version check passed.");

function read(file) {
  return fs.readFileSync(path.join(root, file), "utf8");
}

function readJson(file) {
  return JSON.parse(read(file));
}

function writeJson(file, value) {
  fs.writeFileSync(path.join(root, file), `${JSON.stringify(value, null, 2)}\n`);
}

function replace(file, pattern, replacement) {
  const target = path.join(root, file);
  fs.writeFileSync(target, fs.readFileSync(target, "utf8").replace(pattern, replacement));
}

function updatePackageLock(version) {
  const lockPath = path.join(root, "package-lock.json");
  if (!fs.existsSync(lockPath)) return;
  const lock = readJson("package-lock.json");
  lock.version = version;
  if (lock.packages?.[""]) {
    lock.packages[""].version = version;
  }
  writeJson("package-lock.json", lock);
}

function bump(version, type) {
  const [major, minor, patch] = parse(version);
  if (type === "major") return `${major + 1}.0.0`;
  if (type === "minor") return `${major}.${minor + 1}.0`;
  return `${major}.${minor}.${patch + 1}`;
}

function parse(version) {
  const parts = version.split(".").map(Number);
  if (parts.length !== 3 || parts.some(Number.isNaN)) {
    fail(`Invalid SemVer value: ${version}`);
  }
  return parts;
}

function compare(left, right) {
  const a = parse(left);
  const b = parse(right);
  for (let i = 0; i < 3; i += 1) {
    if (a[i] !== b[i]) return a[i] - b[i];
  }
  return 0;
}

function appendChangelog(version, bumpType) {
  const today = new Date().toISOString().slice(0, 10);
  const entry = [
    `## ${version} - ${today}`,
    "",
    `- ${bumpType}: prepared release ${version}.`,
    ""
  ].join("\n");
  fs.appendFileSync(path.join(root, "CHANGELOG.md"), `\n${entry}`);
}

function hasHead() {
  try {
    execFileSync("git", ["rev-parse", "--verify", "HEAD"], {
      cwd: root,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"]
    });
    return true;
  } catch {
    return false;
  }
}

function git(args) {
  return execFileSync("git", args, { cwd: root, encoding: "utf8" }).trim();
}

function fail(message) {
  console.error(message);
  process.exit(1);
}
