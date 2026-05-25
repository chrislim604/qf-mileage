#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";

const required = [
  "AGENTS.md",
  "README.md",
  "CHANGELOG.md",
  "docs/DEVELOPMENT_STANDARD.md",
  "docs/ARCHITECTURE.md",
  "docs/DATA_MODEL.md",
  "docs/PRIVACY.md",
  "docs/OPERATIONS.md",
  "docs/VERSIONING.md",
  "docs/RELEASE_CHECKLIST.md",
  "docs/adr/0001-platform-choice.md"
];

const missing = required.filter((file) => !fs.existsSync(path.join(process.cwd(), file)));
if (missing.length > 0) {
  console.error(`Missing required docs:\n${missing.join("\n")}`);
  process.exit(1);
}

const privacy = fs.readFileSync(path.join(process.cwd(), "docs/PRIVACY.md"), "utf8");
for (const phrase of ["location history", "Google Drive", "advertising", "billing", "encrypted"]) {
  if (!privacy.toLowerCase().includes(phrase.toLowerCase())) {
    console.error(`docs/PRIVACY.md must mention ${phrase}.`);
    process.exit(1);
  }
}

console.log("Docs check passed.");
