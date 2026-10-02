package com.briefai.conversation.service;

import com.briefai.conversation.entity.ChatMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationQueryService {

    private final ChatModel chatModel;

    public ConversationQueryService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String rewriteQuestion(List<ChatMessage> history, String question) {
        if (history == null || history.isEmpty()) {
            return question;
        }

        StringBuilder conversation = new StringBuilder();

        for (ChatMessage message : history) {
            conversation.append(message.getRole())
                    .append(": ")
                    .append(message.getContent())
                    .append("\n");
        }

        String promptText = """
                Rewrite the user's latest question as a standalone
                search query using the conversation history.

                The rewritten query will be used for semantic search
                over the user's documents.

                Rules:
                - Preserve the user's original intent.
                - Resolve references such as "it", "that",
                  "the second point", "in above response" or "what you said earlier".
                - Do not answer the question.
                - Do not add facts that are not present.
                - Return only the rewritten query.

                Conversation history:
                %s

                Latest question:
                %s
                """.formatted(conversation, question);

        Prompt prompt = new Prompt(new UserMessage(promptText));
        ChatResponse response = chatModel.call(prompt);

        String rewrittenQue = response.getResult().getOutput().getText();

        if (rewrittenQue == null || rewrittenQue.isBlank()) {
            return question;
        }

        return rewrittenQue.trim();
    }
}
