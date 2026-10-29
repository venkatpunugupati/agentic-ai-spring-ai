# Spring AI Port of the ReAct Agent

The raw Python ReAct agent rebuilt on Spring AI.

## Stack

- Spring Boot 4.1.0, Spring AI 2.0.1
- gpt-4o-mini, temperature 0
- Java 27, Maven

## What it is

The same three tools as the original agent, ported as `@Tool` POJOs:

- Calculator, word count, current time

Wiring: `ChatClient` + `MessageChatMemoryAdvisor` (routed by
`CONVERSATION_ID`) + `SimpleLoggerAdvisor`. The agent loop itself is
framework-handled — the ReAct/tool loop is commodity. What stayed mine:
`GroundingVerificationAdvisor`, a custom advisor that verifies temporal
claims against actual tool calls instead of trusting the answer.

## Run it

export OPENAI_API_KEY="..."
mvn spring-boot:run

Note: memory here is `InMemoryChatMemoryRepository` — conversations do not
survive a restart. That is expected, not a bug; the file-backed memory store
lives in the LangChain4j port.

## Verified behavior

- Chained question (word count, then multiply) → 27, clean sequential
  tool chain.
- Unattached tool → honest refusal with zero `toolCalls` in the trace: a
  tool bean not passed to `.tools(...)` is silently absent from the
  model-visible schema.
- Tool-call limit probe (`max-total-tool-calls=1`) → graceful final
  response, confirming Spring AI 2.0.1's shipped 40-per-tool / 150-per-turn
  loop cap.

## Related upstream work

spring-projects/spring-ai#7198 — documentation bug in
`ToolCallingAdvisor.advisorOrder` javadoc (sort direction inverted),
found while positioning the grounding advisor.
