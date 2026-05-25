#!/usr/bin/env node
import { execFileSync } from "node:child_process";

const commands = [
  ["npm", ["run", "docs:check"]],
  ["npm", ["run", "version:check"]],
  ["node", ["scripts/run-gradle.mjs", ":shared:allTests"]]
];

for (const [bin, args] of commands) {
  execFileSync(bin, args, { stdio: "inherit" });
}

console.log("Release preparation checks passed.");
