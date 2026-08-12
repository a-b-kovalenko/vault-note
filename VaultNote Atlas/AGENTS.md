# VaultNote Atlas agent guidance

Before editing this vault, read the shared Markdown and Obsidian standard:
`/Users/akovalenko/Projects/Personal/engineering-standards/obsidian/AGENT_RULES.md`.

## Vault scope

- `VaultNote Atlas/` is the Obsidian vault root. Open this directory in
  Obsidian, not the repository root.
- Keep the vault intentionally small and Ukrainian-only. Technical names,
  identifiers, and code remain in their original form.
- Keep Ukrainian learning explanations separate from public project
  documentation in `docs/`. Promote stable architecture decisions to ADRs;
  do not duplicate the complete implementation documentation in the vault.

## Structure and navigation

```text
VaultNote Atlas/
├── AGENTS.md             local vault instructions
├── VaultNote_Atlas.md    central MOC and navigation
├── content/              all learning notes
└── assets/               shared images and diagrams
```

- Use `VaultNote_Atlas.md` as the only MOC. Group notes there with headings
  instead of creating domain sub-MOCs prematurely.
- Store all learning notes directly in `content/`.
- Capture questions from conversations as draft notes in `content/`, then
  refine them and register them in the central MOC.
- Do not add the Java-KB-specific EN/UK mirror, Quiz, or interview-case
  structure unless the project scope explicitly grows to require it.

## Note conventions

- Each new content note begins with
  `[⬅️](../VaultNote_Atlas.md)`, followed by `## 📝 TL;DR`.
- Do not add an H1 to the note body; the filename or vault UI supplies the
  title.
- Use standard relative Markdown links with the `.md` extension. Do not use
  Obsidian wiki-links.

## Assets and diagrams

- Store images and other static assets in `VaultNote Atlas/assets/`.
- Use Mermaid for architecture, flow, lifecycle, and relationship diagrams.
- Do not use ASCII pseudographics. Prefer compact top-to-bottom diagrams.
