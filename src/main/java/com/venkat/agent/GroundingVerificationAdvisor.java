package com.venkat.agent;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/**
 * Week 2 check_grounding, rehoused as a Spring AI advisor.
 * Sits one slot inside ToolCallingAdvisor's loop, so it sees every iteration.
 * Fires at most once per turn -- the bound is evidence-based: the synthetic
 * correction carries a marker id ("grounding-...") visible in the transcript
 * on the next iteration. No instance state (singleton bean).
 */
public class GroundingVerificationAdvisor implements CallAdvisor {

    private static final Pattern TEMPORAL =
            Pattern.compile("time|date|today|now", Pattern.CASE_INSENSITIVE);
    private static final String MARKER_PREFIX = "grounding-";
    private static final String TIME_TOOL = "getCurrentTime";

    @Override
    public String getName() {
        return "GroundingVerificationAdvisor";
    }

    @Override
    public int getOrder() {
        return ToolCallingAdvisor.DEFAULT_ORDER + 1;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        if (response.chatResponse() == null) {
            return response;
        }

        // (a) temporal? -- the current turn's question only (last UserMessage,
        //     not history: turn 1 being temporal must not arm turn 2)
        if (!isTemporalTurn(request)) {
            return response;
        }

        // (b) evidence scan over the turn transcript
        if (alreadyCorrected(request) || hasTimeEvidence(request)) {
            return response;
        }

        // (c) fire rule: final response (no tool calls) + temporal + no evidence.
        // Synthesize the corrective tool call; ToolCallingAdvisor executes it
        // (eligibility defaults to hasToolCalls()) and the loop iterates.
        AssistantMessage output = response.chatResponse().getResult().getOutput();
        if (output.hasToolCalls()) {
            return response; // not final -- loop continues on its own
        }
        return syntheticTimeCall(response);
    }

    private boolean isTemporalTurn(ChatClientRequest request) {
        List<Message> messages = request.prompt().getInstructions();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage user) {
                return TEMPORAL.matcher(user.getText()).find();
            }
        }
        return false;
    }

    private boolean alreadyCorrected(ChatClientRequest request) {
        return request.prompt().getInstructions().stream()
                .filter(AssistantMessage.class::isInstance)
                .map(AssistantMessage.class::cast)
                .flatMap(m -> m.getToolCalls().stream())
                .map(AssistantMessage.ToolCall.class::cast)
                .anyMatch(tc -> tc.id().startsWith(MARKER_PREFIX));
    }

    private boolean hasTimeEvidence(ChatClientRequest request) {
        return request.prompt().getInstructions().stream()
                .filter(ToolResponseMessage.class::isInstance)
                .map(ToolResponseMessage.class::cast)
                .flatMap(m -> m.getResponses().stream())
                .anyMatch(r -> TIME_TOOL.equals(r.name()));
    }

    private ChatClientResponse syntheticTimeCall(ChatClientResponse from) {
        AssistantMessage synthetic = AssistantMessage.builder()
                .content("Grounding correction: call getCurrentTime and answer from its result.")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        MARKER_PREFIX + System.nanoTime(), "function", TIME_TOOL, "{}")))
                .build();
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(synthetic)));
        return new ChatClientResponse(chatResponse, from.context());
    }
}
