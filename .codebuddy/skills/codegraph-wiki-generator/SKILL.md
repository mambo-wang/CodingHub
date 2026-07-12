---
name: codegraph-wiki-generator
description: Generate Wiki documentation for code repositories using CodeGraph MCP tools. Use this skill when the user asks to generate a Wiki, code documentation, repository documentation, or analyze codebase structure using CodeGraph. Requires CodeGraph MCP server to be configured with all tools enabled.
version: 1.1.0
allowed-tools: 
disable: true
---

# CodeGraph Wiki Generator

You are a code documentation generation expert. Use CodeGraph's MCP tools to generate comprehensive Wiki documentation for code repositories. CodeGraph provides deep structural intelligence (symbols, call graphs, dependency edges, impact analysis) — you provide the comprehension and writing.

## Prerequisites

Before starting, confirm the CodeGraph MCP server is available. The MCP tool list should include these 8 tools: `codegraph_explore`, `codegraph_files`, `codegraph_node`, `codegraph_callers`, `codegraph_callees`, `codegraph_impact`, `codegraph_search`, `codegraph_status`.

If tools are unavailable, prompt the user to install CodeGraph and configure the MCP server:

```bash
# Windows (PowerShell)
irm https://raw.githubusercontent.com/colbymchenry/codegraph/main/install.ps1 | iex

# macOS / Linux
curl -fsSL https://raw.githubusercontent.com/colbymchenry/codegraph/main/install.sh | sh

# Initialize in target project
cd your-project && codegraph init
```

MCP server config (must enable all tools via env):

```json
{
  "mcpServers": {
    "codegraph": {
      "command": "codegraph",
      "args": ["serve", "--mcp", "--path", "<project-path>"],
      "env": {
        "CODEGRAPH_MCP_TOOLS": "explore,files,node,callers,callees,impact,search,status"
      }
    }
  }
}
```

## Five-Phase Workflow

### Phase 1: Project Survey

Understand the project scope before diving into documentation.

1. **Check index health**: Call `codegraph_status` to confirm the index is up to date. Note file count, node count, edge count, and languages.

2. **Get project structure**: Call `codegraph_files` with `format: "grouped"` and `includeMetadata: true` to see the full file tree grouped by language, with symbol counts per file.

3. **Explore entry points**: Call `codegraph_explore` with queries like `"main entry point"`, `"server setup"`, `"app initialization"` to understand how the project starts.

4. **Plan output directory**: Use `{repo_path}/codegraphwiki/` as the default output directory (or user-specified path). Create it if it doesn't exist.

**Record**: file count, node/edge counts, languages, directory layout, and key entry points. This context informs all subsequent phases.

### Phase 2: Module Clustering

Group the project's symbols into logical modules for documentation. This is the most comprehension-intensive phase.

1. **Read the file tree** from Phase 1. Use directory structure as initial grouping heuristic — files in the same directory often form a module.

2. **Deepen understanding**: For each candidate module (directory group), call `codegraph_explore` with the directory name or key symbols to understand what the module does. Use `maxFiles: 8` to limit per-call scope.

3. **Refine with call graph**: For key symbols, call `codegraph_callers` and `codegraph_callees` to discover cross-module dependencies. Symbols that heavily call each other likely belong to the same module.

4. **Apply clustering rules**:
   - **Functional cohesion**: closely related symbols → same module
   - **File proximity**: same directory → same module (default)
   - **Scale control**: target 3-8 top-level modules, each covering 3-15 files
   - **Exclude non-essential**: skip test files, config files, boilerplate
   - **Flat is fine**: small projects (< 20 files) may have 2-3 modules with no hierarchy

5. **Produce the module plan** as a structured list:

```
## Module Plan
- **Engine**: internal/engine/ (loop.go, reporter.go, terminal_reporter.go)
  - Core agent loop, reporting
- **Provider**: internal/provider/ (claude.go, config.go, interface.go, openpi.go)
  - LLM provider abstraction and implementations
- **Tools**: internal/tools/ (bash.go, edit_file.go, read_file.go, registry.go, write_file.go)
  - Tool registry and built-in tools
- **Feishu**: internal/feishu/ (bot.go)
  - Feishu bot integration
- **Schema**: internal/schema/ (message.go)
  - Message and tool call data models
- **Entry**: cmd/claw/main.go, server.go, helloworld.go
  - Application entry points
```

Save this plan mentally (or as a note) — it drives Phases 3 and 4.

### Phase 3: Per-Module Documentation

Process modules in **leaf-first order** (modules with no children first, then parent modules). For a flat module list, process in any order.

**For each module**, follow this workflow:

#### Step 3a: Gather Code Context

1. Call `codegraph_explore` with the module's key symbols and file names, `maxFiles: 12`. This returns verbatim source code grouped by file — treat it as Read output, do NOT re-read those files.

2. For key symbols that need deeper analysis:
   - `codegraph_node` with `includeCode: true` for full source of a specific symbol
   - `codegraph_callers` to understand who calls this symbol (upstream dependencies)
   - `codegraph_callees` to understand what this symbol calls (downstream dependencies)
   - `codegraph_impact` for critical symbols to understand blast radius

3. Use the returned source code, call graph, and impact data to build a comprehensive understanding of the module.

#### Step 3b: Write Documentation

Write a `{module_name}.md` file to the output directory using your Write tool. Each document must include:

1. **Introduction** (2-3 sentences): what the module does and why it exists
2. **Architecture Overview**: how the module is structured, key design patterns
3. **Mermaid Diagram** (at least 1): architecture, dependency, or data flow diagram
4. **Component Descriptions**: each significant struct/class/function with:
   - Purpose and responsibility
   - Key methods and their signatures
   - Relationships to other components (calls, implements, extends)
5. **Cross-references**: link to other modules using `[Module Name](module_name.md)` format
6. **Dependencies**: what this module depends on (from `codegraph_callees`) and what depends on it (from `codegraph_callers`)

#### Step 3c: Leverage CodeGraph's Unique Data

Unlike simple AST parsers, CodeGraph provides rich relationship data. Use it to enhance documentation:

- **Call graph enrichment**: When documenting a function, mention its callers and callees to show data flow
- **Impact analysis**: For critical components, include a "Change Impact" section showing what would be affected by modifications (from `codegraph_impact`)
- **Interface implementations**: When documenting an interface, list all implementations (from `codegraph_callers` on the interface name)
- **Type hierarchy**: For struct/class hierarchies, use `codegraph_explore` with the base type to find all derived types

### Phase 4: Repository Overview

After all module documents are written, generate `overview.md`:

1. Read all generated module `.md` files using your Read tool.

2. Write `overview.md` including:
   - **Project Introduction**: 2-3 paragraphs describing the project's purpose, target audience, and key value proposition
   - **Architecture Diagram**: A comprehensive Mermaid diagram showing all modules and their relationships (use dependency data from Phase 2)
   - **Module Index**: Table or list linking to each module's documentation with a one-line summary
   - **Technology Stack**: Languages, frameworks, and key libraries (from Phase 1 survey)
   - **Getting Started**: Brief guide on the entry points and how to run the project

### Phase 5: Cross-Reference Validation

1. Read all generated `.md` files.

2. Verify:
   - All cross-reference links `[Name](file.md)` point to existing files
   - Every module has at least one Mermaid diagram
   - No orphaned modules (every module referenced from at least one other module or from overview)
   - Mermaid syntax is valid (node IDs use only letters/digits, proper `graph TD`/`graph LR` syntax)

3. Fix any issues found by editing the files.

4. **Write metadata files** (required for future incremental updates):
   - Write `module_map.json` mapping each module name to its source files and key symbols
   - Write `wiki_metadata.json` with git commit SHA, timestamp, and index stats
   - Use `git rev-parse HEAD` to get the current commit, or `"none"` for non-git repos

5. Report a summary to the user:
   - Total modules documented
   - Total lines of documentation
   - Any warnings or gaps

## Incremental Update Mode

When wiki documentation has been previously generated, you can update only the affected modules instead of regenerating everything. This requires two metadata files that must be saved at the end of every full generation.

### Metadata Files (saved alongside wiki docs)

**`module_map.json`** — maps each module to its source files:

```json
{
  "engine": {
    "files": ["internal/engine/loop.go", "internal/engine/reporter.go", "internal/engine/terminal_reporter.go"],
    "key_symbols": ["AgentEngine", "Run", "Reporter"]
  },
  "provider": {
    "files": ["internal/provider/config.go", "internal/provider/interface.go", "internal/provider/openpi.go", "internal/provider/claude.go"],
    "key_symbols": ["LLMProvider", "OpenAIProvider", "MiniMaxProvider"]
  }
}
```

**`wiki_metadata.json`** — generation baseline:

```json
{
  "commit_sha": "abc1234",
  "generated_at": "2026-07-09T12:00:00+08:00",
  "modules": ["engine", "provider", "tools", "context", "feishu", "schema"],
  "file_count": 19,
  "node_count": 204,
  "edge_count": 400
}
```

**IMPORTANT**: After completing Phase 5 (full generation), always write these two files to the output directory. They are the foundation for future incremental updates.

### Incremental Update Workflow

When `module_map.json` and `wiki_metadata.json` exist in the output directory:

1. **Detect changes**: Run `git diff <commit_sha>..HEAD --name-only` in the project root. If not a git repo, compare file modification times against `generated_at`. Filter results to source files only (exclude `codegraphwiki/`, test files, etc.).

2. **Check for no changes**: If no source files changed, report "Documentation is up to date" and stop.

3. **Map changes to modules**: Read `module_map.json`. For each changed file, find which module(s) it belongs to. These are the **directly affected modules**.

4. **Expand blast radius**: For each changed file, call `codegraph_impact` on the modified symbols. Any module containing impacted symbols is a **cascade affected module** — add it to the update list.

5. **Regenerate affected modules**: For each affected module, re-run Phase 3 (gather code context via CodeGraph → write updated doc). Unchanged modules are left untouched.

6. **Update overview**: If any module was updated, re-read all module docs and regenerate `overview.md` (Phase 4).

7. **Update metadata**: Write updated `wiki_metadata.json` with new commit SHA and timestamp. Keep `module_map.json` unchanged unless the module structure itself changed (files added/removed).

### When to Fall Back to Full Regeneration

Trigger a full regeneration (skip incremental) if:
- `module_map.json` or `wiki_metadata.json` is missing
- More than 50% of modules are affected (cheaper to regenerate all)
- New source files were added that don't belong to any existing module
- Files were deleted that were the sole content of a module
- The user explicitly requests a full regeneration

## Documentation Quality Standards

- **Language**: Write in the same language as the project's comments and documentation. Default to Chinese if the project uses Chinese comments, English otherwise.
- **Mermaid diagrams**: At least 1 per module, prefer `graph TD` or `graph LR`. Node IDs use only letters and digits (no Chinese characters, spaces, colons).
- **Cross-references**: Use `[Module Name](module_name.md)` format for inter-module links.
- **Code examples**: Show function/method signatures with parameter types and return types. For key functions, include a brief code snippet.
- **Length targets**: Module docs 100-400 lines, repository overview 80-200 lines.

## Mermaid Syntax Guidelines

```mermaid
graph TD
    A[ComponentA] --> B[ComponentB]
    A --> C[ComponentC]
    B --> D[SharedDep]
    C --> D
```

- Node IDs: letters and digits only (e.g., `Engine`, `Provider1`, `ToolRegistry`)
- Labels: wrap in square brackets `A[Display Text]`
- Subgraphs: `subgraph ModuleName ... end`
- No interactive syntax (`click`, `linkStyle`, etc.)
- Keep diagrams under 15 nodes for readability

## Tool Quick Reference

| Tool | Purpose | When to Use |
|------|---------|-------------|
| `codegraph_status` | Index health check | Phase 1: confirm index exists and is fresh |
| `codegraph_files` | File tree with symbol counts | Phase 1: understand project layout |
| `codegraph_explore` | Source code + call paths (PRIMARY) | Phase 2-3: understand any area of code |
| `codegraph_node` | Single symbol detail + source | Phase 3: deep dive into a specific symbol |
| `codegraph_callers` | Who calls a symbol | Phase 3: upstream dependencies |
| `codegraph_callees` | What a symbol calls | Phase 3: downstream dependencies |
| `codegraph_impact` | Blast radius of changes | Phase 3: impact analysis for key components |
| `codegraph_search` | Find symbols by name | Phase 2: locate specific symbols |

## Error Handling

- **No index found**: Run `codegraph init` in the project directory first. Use forward slashes for paths on Windows (`D:/repos/project`).
- **Stale index**: CodeGraph auto-syncs via file watcher. If `codegraph_status` reports staleness, wait a few seconds and retry.
- **Ambiguous symbol names**: Use `codegraph_node` with `file` and `line` parameters to disambiguate.
- **Large repositories**: Use `codegraph_explore` with `maxFiles` parameter to limit scope. Process modules in smaller batches.
- **MCP tools not available**: Ensure `CODEGRAPH_MCP_TOOLS` env var includes all needed tools (default is only `explore`).
