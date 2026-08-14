# Avenra working rules

## Purpose

Avenra is a portfolio/CV Android E-commerce application. The goal is a polished, credible product that demonstrates strong Android engineering; it is not a production commercial platform.

## Source of truth

- Treat the audited Figma as the primary visual and UI/UX baseline, not a complete product specification.
- The Figma contains legacy Route branding. Do not claim it has been updated; replace application-identity branding with Avenra in the Android implementation when that phase is approved.
- Preserve verified Figma flows where applicable and add only product gaps that have meaningful value.
- Keep factual decisions in `docs/project-context.md`; mark anything not agreed as **TBD / NOT DECIDED**.

## Working approach

Follow controlled phases: analyze, discuss, agree decisions, define the phase, implement that phase, build, test, review, fix verified issues, then continue.

Do not implement the full application at once. Do not introduce technologies, architectural layers, abstractions, libraries, APIs, backend/database choices, or features without a justified and approved requirement.

Prioritize correctness, then simplicity, maintainability, testability, performance, and scalability. Avoid overengineering, duplicated components, God classes, God ViewModels, and UI-owned business logic where separation is appropriate.

## Product boundaries

- The Android app will consume an owned REST API; it must not use a third-party product API as its production data source.
- Payment is mock/simulated only. Never represent it as real financial processing.
- Promotional banners are store content that may come from the owned API; do not add third-party advertising without a changed requirement.
- Do not add excluded or deferred capabilities unless requirements change.

## Documentation and Git

Keep documentation concise and update it when material decisions become final. Never run `git commit`, `git push`, or `git tag` unless explicitly requested.
