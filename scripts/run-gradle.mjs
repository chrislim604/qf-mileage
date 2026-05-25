#!/usr/bin/env node
import { spawnSync } from "node:child_process";
import fs from "node:fs";

const args = process.argv.slice(2);
const env = { ...process.env };

if (!env.JAVA_HOME) {
  const candidates = [
    "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home",
    "/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home",
    "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
  ];
  const detected = candidates.find((candidate) => fs.existsSync(candidate));
  if (detected) {
    env.JAVA_HOME = detected;
  }
}

const result = spawnSync("./gradlew", args, {
  env,
  stdio: "inherit"
});

process.exit(result.status ?? 1);
