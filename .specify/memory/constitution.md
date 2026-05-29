<!--
Sync Impact Report
==================
Version Change:  N/A → 0.1.0 (initial creation, MINOR)
Modified Principles: N/A (all new)
Added Sections:
  - I. Code Quality Standards
  - II. Testing Standards
  - III. User Experience Consistency
  - IV. Performance Requirements
  - V. Observability
  - VI. Simplicity & YAGNI
Removed Sections: N/A
Templates Updated:
  ✅ .specify/templates/plan-template.md (Constitution Check section already aligned)
  ✅ .specify/templates/spec-template.md (no constitution references to update)
  ✅ .specify/templates/tasks-template.md (no constitution references to update)
  ✅ .specify/templates/commands/*.md (no outdated agent references found)
Follow-up TODOs:
  - TODO(RATIFICATION_DATE): Ratification date not known — set before first formal review
-->

# iaihub Constitution

## Core Principles

### I. Code Quality Standards

Code quality is the foundation of sustainable development. All code contributions MUST meet the following standards:

- **Readability**: Code must be self-documenting with clear naming conventions. Complex logic requires inline comments explaining the "why", not the "what".
- **Consistency**: Adopt a uniform code style across the entire codebase. Use automated linting and formatting tools (e.g., ESLint, Prettier, Black, Rustfmt) to enforce consistency.
- **Maintainability**: Modules MUST be small, focused, and single-responsibility. Avoid god objects/files; prefer composition over inheritance.
- **Type Safety**: Prefer statically typed languages or strict type annotations. All public interfaces MUST have explicit types; internal types SHOULD be explicit where they improve clarity.
- **Error Handling**: Methods MUST NOT return null. Use Optional wrapping or exceptions to signal absence or failure. Never silently swallow exceptions without logging.

### II. Testing Standards

Testing is a non-negotiable requirement for all feature work. Quality gates enforce correctness and enable safe refactoring.

- **Test-Driven Development (TDD)**: For new features, tests MUST be written before implementation. Red-Green-Refactor cycle strictly enforced: write a failing test → implement the minimum code to pass → refactor.
- **Test Coverage**: Minimum 80% line coverage for new code. Critical paths (authentication, data mutations, payment flows) require 100% coverage.
- **Test Categories**:
  - **Unit Tests**: Isolated, fast, test a single function/class. Must be deterministic with no external dependencies (database, network, filesystem).
  - **Contract Tests**: Verify API/service interfaces behave as specified. Required when exposing or consuming shared APIs.
  - **Integration Tests**: Verify multi-component workflows. Use real dependencies (test database, in-memory services) to confirm system integration.
- **Test Quality**: Tests MUST be independent, repeatable, and self-contained. No shared mutable state between tests. Each test verifies one logical assertion.
- **Automated CI Gates**: All tests MUST pass in CI before merge. Coverage regression below threshold blocks merge.

### III. User Experience Consistency

A cohesive user experience builds trust and reduces cognitive load. All user-facing outputs MUST adhere to these principles:

- **Visual Consistency**: Follow established design system patterns (colors, typography, spacing, components). Do not introduce ad-hoc UI patterns without design review.
- **Interaction Consistency**: Similar actions produce similar results across the product. Keyboard shortcuts, gestures, and navigation flows MUST behave identically across equivalent contexts.
- **Feedback Consistency**: Every user action MUST produce visible feedback (loading indicators, success/error messages, state changes). Never leave users uncertain about whether an action succeeded.
- **Accessibility (a11y)**: All interactive elements MUST be keyboard accessible and screen-reader friendly. Follow WCAG 2.1 AA minimum. Color contrast MUST meet threshold requirements.
- **Error Messaging**: Error messages MUST be human-readable, actionable, and avoid technical jargon. Users MUST always know what happened and what to do next.

### IV. Performance Requirements

Performance is a feature. Slow systems erode user trust and productivity. Every feature MUST consider its performance contract.

- **Response Time**:
  - API endpoints: p95 latency < 200ms for read operations, < 500ms for write operations.
  - UI interactions: 60fps for animations and transitions; input response < 100ms.
  - Page/screen load: First contentful paint < 1.5s on representative hardware and network conditions.
- **Resource Efficiency**: No memory leaks. Periodic long-running operations MUST release resources. Monitor heap usage in tests and CI.
- **Scalability**: Design assumes 10× current load. Optimize hot paths for the 95th percentile workload, not the average.
- **Offline/Async**: Operations that can be asynchronous MUST not block the main thread. Provide offline-capable patterns where appropriate.
- **Performance Testing**: Critical paths MUST have performance tests that enforce latency budgets in CI.

### V. Observability

Systems MUST be observable in production. Debugging without logs is unacceptable.

- **Structured Logging**: All services MUST emit structured logs (JSON format preferred). Logs MUST include: timestamp, severity, correlation ID, service name, and action.
- **Log Levels**: ERROR for failures requiring attention; WARN for degraded states; INFO for significant business events; DEBUG for development detail (must be silenced in production).
- **Metrics**: Expose key health and business metrics (request rate, error rate, latency percentiles, throughput). Use a consistent metrics schema across all services.
- **Tracing**: Distributed requests MUST carry a correlation ID through all services. Trace propagation MUST be implemented for all inter-service communication.
- **Alerting**: Define SLOs/SLIs and configure alerts on threshold breaches. Alerts MUST be actionable, not noisy.

### VI. Simplicity & YAGNI

Complexity is the enemy of reliability and maintainability. Every line of code, abstraction, and dependency is a liability.

- **YAGNI (You Aren't Gonna Need It)**: Do not implement features or abstractions until they are explicitly required. Resist speculative generalization.
- **Start Simple**: Prefer the simplest solution that solves the problem. Introduce complexity only when complexity is forced by a concrete requirement.
- **Dependency Discipline**: Regularly audit and remove unused dependencies. A new dependency requires justification and review. Prefer standard library or well-maintained packages.
- **Avoid Over-Engineering**: Do not build configurable frameworks, plugin systems, or abstraction layers until the need is demonstrated. Prefer convention over configuration.
- **Technical Debt**: Track known debt explicitly. Allocate dedicated time for debt reduction. Unaddressed debt accumulating without a remediation plan MUST be escalated.

## Additional Constraints

### Technology Stack Boundaries

- **Backend**: Python 3.11+, Node.js 20+ (or language explicitly chosen per feature)
- **Frontend**: React 18+ with TypeScript (strict mode), or framework explicitly chosen per feature
- **Database**: PostgreSQL preferred for relational data; object storage for files/blobs
- **Infrastructure**: Containerized deployments (Docker/OCI). Cloud-agnostic where possible.
- **No New transitive dependencies** without explicit approval. Lockfile MUST be updated on dependency changes.

### Security Requirements

- **Authentication**: All user-facing endpoints MUST require authentication. Use industry-standard auth (OAuth 2.0, JWT with short expiry + refresh).
- **Secrets Management**: Secrets MUST NEVER be committed to version control. Use environment variables or secret management services.
- **Input Validation**: All external input MUST be validated and sanitized before processing. Never trust user input.
- **Dependency Scanning**: Automated security scanning (e.g., Dependabot, Snyk) MUST be enabled. Critical vulnerabilities MUST be patched within 24 hours.
- **Principle of Least Privilege**: Services and users MUST have only the permissions they strictly require.

## Development Workflow

### Feature Development Lifecycle

1. **Specify** (`/specify`): Define requirements in plain language. Focus on user value and acceptance criteria.
2. **Plan** (`/plan`): Translate requirements into a technical implementation plan with architecture, data models, and API contracts.
3. **Tasks** (`/tasks`): Break work into independently implementable and testable tasks, organized by user story.
4. **Implement**: Execute tasks following TDD. Each user story MUST be independently testable and demonstrable.
5. **Review**: Code review verifies compliance with this constitution. Reviewers MUST check code quality, test coverage, and UX consistency.
6. **Checklist**: Pre-merge checklist verifies all gates pass (tests, linting, coverage, accessibility, performance).

### Code Review Requirements

- Every change MUST have at least one independent reviewer.
- Reviews MUST cover: correctness, test quality, code style compliance, security implications, and constitutional adherence.
- Unresolved objections from a reviewer block merge.
- Reviews SHOULD complete within one business day of request.

### Definition of Done

A feature is DONE only when:
- All acceptance criteria from the specification are met
- All tests pass (unit, contract, integration)
- Coverage meets the minimum threshold
- No linting or type errors
- Product Owner or delegated stakeholder has approved the demonstration
- Performance meets defined budgets

## Governance

This constitution is the supreme authority over development practices in this project. All other practices, templates, and conventions are subordinate to it.

### Amendment Procedure

1. Proposed amendment MUST be documented with rationale and impact analysis
2. Amendment requires approval from project lead or designated authority
3. Migration plan (if breaking) MUST accompany the amendment
4. Version MUST be incremented according to semantic versioning rules:
   - **MAJOR**: Backward-incompatible governance changes, principle removals or redefinitions
   - **MINOR**: New principles or materially expanded guidance
   - **PATCH**: Clarifications, wording fixes, non-semantic refinements
5. All team members MUST be notified of changes before they take effect

### Compliance

- All pull requests and code reviews MUST verify compliance with this constitution
- Non-compliant contributions MUST NOT be merged until corrected
- Complexity deviations MUST be justified and documented in the implementation plan
- Constitution violations discovered post-merge MUST be tracked as urgent tech debt and fixed within one sprint

### Runtime Guidance

For day-to-day development guidance, refer to:
- `.specify/templates/spec-template.md` — feature specification format
- `.specify/templates/plan-template.md` — implementation planning format
- `.specify/templates/tasks-template.md` — task breakdown format
- Project documentation (`docs/`, `README.md`) — domain-specific guidance

**Version**: 0.1.0 | **Ratified**: TODO(RATIFICATION_DATE) | **Last Amended**: 2026-05-29