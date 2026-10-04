import { useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";

import {
  createConversation,
  deleteConversation,
  getConversationMessages,
  getConversations,
} from "../api/conversationApi";

import { askQuestion } from "../api/chatApi";
import { getApiErrorMessage } from "../api/apiError";
import SourceCards from "../components/SourceCards";
import CreateConversationModal from "../components/CreateConversationModal";

function formatConversationDate(value) {
  if (!value) {
    return "";
  }

  const date = new Date(value);
  const now = new Date();

  const sameDay = date.toDateString() === now.toDateString();

  if (sameDay) {
    return new Intl.DateTimeFormat("en", {
      hour: "numeric",
      minute: "2-digit",
    }).format(date);
  }

  return new Intl.DateTimeFormat("en", {
    day: "numeric",
    month: "short",
  }).format(date);
}

function formatMessageTime(value) {
  if (!value) {
    return "";
  }

  return new Intl.DateTimeFormat("en", {
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(value));
}

export default function ChatPage() {
  const bottomRef = useRef(null);
  const textareaRef = useRef(null);
  const [searchParams, setSearchParams] = useSearchParams();

  const [conversations, setConversations] = useState([]);

  const [activeConversation, setActiveConversation] = useState(null);

  const [messages, setMessages] = useState([]);

  const [question, setQuestion] = useState("");

  const [loadingConversations, setLoadingConversations] = useState(true);

  const [loadingMessages, setLoadingMessages] = useState(false);

  const [sending, setSending] = useState(false);

  const [error, setError] = useState("");
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [creatingConversation, setCreatingConversation] = useState(false);

  const loadConversations = async () => {
    try {
      setError("");

      const data = await getConversations();

      setConversations(data);

      return data;
    } catch (err) {
      setError(getApiErrorMessage(err));
      return [];
    } finally {
      setLoadingConversations(false);
    }
  };

  useEffect(() => {
    loadConversations();
  }, []);

  useEffect(() => {
    if (searchParams.get("new") === "1") {
      setShowCreateModal(true);
      setSearchParams({}, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({
      behavior: "smooth",
    });
  }, [messages, sending]);

  const selectConversation = async (conversation) => {
    if (sending) {
      return;
    }

    setActiveConversation(conversation);
    setMessages([]);
    setError("");
    setLoadingMessages(true);

    try {
      const data = await getConversationMessages(conversation.id);

      setMessages(data);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setLoadingMessages(false);
    }
  };

  const handleNewConversation = () => {
    setError("");
    setShowCreateModal(true);
  };

  const handleCreateConversation = async (title) => {
    try {
      setCreatingConversation(true);
      setError("");

      const created = await createConversation(title);

      setConversations((current) => [created, ...current]);
      setActiveConversation(created);
      setMessages([]);
      setShowCreateModal(false);

      setTimeout(() => {
        textareaRef.current?.focus();
      }, 0);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setCreatingConversation(false);
    }
  };

  const handleDeleteConversation = async (event, conversation) => {
    event.stopPropagation();

    if (sending) {
      return;
    }

    const confirmed = window.confirm(
      `Delete "${conversation.title}" and its message history?`,
    );

    if (!confirmed) {
      return;
    }

    try {
      setError("");

      await deleteConversation(conversation.id);

      setConversations((current) =>
        current.filter((item) => item.id !== conversation.id),
      );

      if (activeConversation?.id === conversation.id) {
        setActiveConversation(null);
        setMessages([]);
      }
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const cleanQuestion = question.trim();

    if (!cleanQuestion || !activeConversation || sending) {
      return;
    }

    if (cleanQuestion.length > 2000) {
      setError("Question must not exceed 2000 characters.");
      return;
    }

    const optimisticUserMessage = {
      id: `temp-user-${Date.now()}`,
      role: "USER",
      content: cleanQuestion,
      createdAt: new Date().toISOString(),
      sources: [],
    };

    setQuestion("");
    setError("");

    setMessages((current) => [...current, optimisticUserMessage]);

    try {
      setSending(true);

      const response = await askQuestion(activeConversation.id, cleanQuestion);

      const assistantMessage = {
        id: `temp-assistant-${Date.now()}`,
        role: "ASSISTANT",
        content: response.answer,
        createdAt: new Date().toISOString(),
        sources: response.sources || [],
      };

      setMessages((current) => [...current, assistantMessage]);

      // The backend touches updatedAt when the
      // exchange is persisted, so refresh ordering.
      const refreshed = await loadConversations();

      const refreshedActive = refreshed.find(
        (conversation) => conversation.id === activeConversation.id,
      );

      if (refreshedActive) {
        setActiveConversation(refreshedActive);
      }
    } catch (err) {
      // Remove optimistic user message because
      // backend persists the exchange atomically.
      setMessages((current) =>
        current.filter((message) => message.id !== optimisticUserMessage.id),
      );

      setError(getApiErrorMessage(err));
    } finally {
      setSending(false);

      setTimeout(() => {
        textareaRef.current?.focus();
      }, 0);
    }
  };

  const handleTextareaKeyDown = (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();

      event.currentTarget.form?.requestSubmit();
    }
  };

  return (
    <div className="chat-page">
      <aside className="conversation-panel">
        <div className="conversation-panel-header">
          <div>
            <span className="conversation-eyebrow">WORKSPACE</span>

            <h2>Conversations</h2>
          </div>

          <button className="new-chat-button" onClick={handleNewConversation}>
            +
          </button>
        </div>

        <button
          className="new-conversation-main"
          onClick={handleNewConversation}
        >
          + New conversation
        </button>

        <div className="conversation-list">
          {loadingConversations ? (
            <div className="conversation-loading">Loading conversations...</div>
          ) : conversations.length === 0 ? (
            <div className="conversation-empty">
              <p>No conversations yet.</p>

              <span>Start a chat with your documents.</span>
            </div>
          ) : (
            conversations.map((conversation) => (
              <button
                key={conversation.id}
                className={`conversation-item ${
                  activeConversation?.id === conversation.id ? "active" : ""
                }`}
                onClick={() => selectConversation(conversation)}
              >
                <div className="conversation-item-content">
                  <span className="conversation-title">
                    {conversation.title}
                  </span>

                  <span className="conversation-date">
                    {formatConversationDate(conversation.updatedAt)}
                  </span>
                </div>

                <span
                  role="button"
                  tabIndex={0}
                  className="conversation-delete"
                  title="Delete conversation"
                  onClick={(event) =>
                    handleDeleteConversation(event, conversation)
                  }
                  onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                      handleDeleteConversation(event, conversation);
                    }
                  }}
                >
                  ×
                </span>
              </button>
            ))
          )}
        </div>
      </aside>

      <section className="chat-workspace">
        {error && (
          <div className="chat-error">
            {error}

            <button type="button" onClick={() => setError("")}>
              ×
            </button>
          </div>
        )}

        {!activeConversation ? (
          <div className="chat-welcome">
            <div className="chat-welcome-icon">B</div>

            <h1>Ask your documents</h1>

            <p>
              Start a conversation and BriefAI will answer using information
              retrieved from your processed documents.
            </p>

            <button className="primary-button" onClick={handleNewConversation}>
              Start a conversation
            </button>

            <div className="chat-capabilities">
              <div>
                <strong>Grounded</strong>
                <span>Answers use your document context.</span>
              </div>

              <div>
                <strong>Context-aware</strong>
                <span>Ask natural follow-up questions.</span>
              </div>

              <div>
                <strong>Cited</strong>
                <span>Trace answers back to sources.</span>
              </div>
            </div>
          </div>
        ) : (
          <>
            <header className="chat-header">
              <div>
                <span className="chat-header-label">CONVERSATION</span>

                <h1>{activeConversation.title}</h1>
              </div>

              <div className="chat-grounded-badge">
                <span />
                Document grounded
              </div>
            </header>

            <div className="messages-container">
              {loadingMessages ? (
                <div className="messages-loading">
                  <div className="loading-circle" />

                  <span>Loading conversation...</span>
                </div>
              ) : messages.length === 0 ? (
                <div className="conversation-start">
                  <div className="conversation-start-icon">◇</div>

                  <h2>What would you like to know?</h2>

                  <p>Ask a question about any of your processed documents.</p>

                  <div className="question-hints">
                    <button
                      onClick={() =>
                        setQuestion("Summarize the key points in my documents.")
                      }
                    >
                      Summarize the key points
                    </button>

                    <button
                      onClick={() =>
                        setQuestion(
                          "What are the most important concepts discussed?",
                        )
                      }
                    >
                      Find important concepts
                    </button>
                  </div>
                </div>
              ) : (
                <div className="messages-list">
                  {messages.map((message) => {
                    const assistant = message.role === "ASSISTANT";

                    return (
                      <div
                        className={`message-row ${
                          assistant ? "assistant" : "user"
                        }`}
                        key={message.id}
                      >
                        <div className="message-avatar">
                          {assistant ? "B" : "You"}
                        </div>

                        <div className="message-body">
                          <div className="message-author">
                            <strong>{assistant ? "BriefAI" : "You"}</strong>

                            <span>{formatMessageTime(message.createdAt)}</span>
                          </div>

                          <div className="message-content">
                            {message.content}
                          </div>

                          {assistant && (
                            <SourceCards
                              sources={message.sources || []}
                              answer={message.content}
                            />
                          )}
                        </div>
                      </div>
                    );
                  })}

                  {sending && (
                    <div className="message-row assistant">
                      <div className="message-avatar">B</div>

                      <div className="message-body">
                        <div className="message-author">
                          <strong>BriefAI</strong>
                        </div>

                        <div className="thinking-indicator">
                          <span />
                          <span />
                          <span />

                          <em>
                            Searching your documents and generating an answer...
                          </em>
                        </div>
                      </div>
                    </div>
                  )}

                  <div ref={bottomRef} />
                </div>
              )}
            </div>

            <div className="chat-composer-area">
              <div className="notice">
                <b>Notice:</b> This application processes data entirely on your
                local machine using Ollama's llama 3.2:3B. Because it runs
                locally without cloud servers, response times and detail levels
                may vary based on your computer's hardware.
              </div>
              <form className="chat-composer" onSubmit={handleSubmit}>
                <textarea
                  ref={textareaRef}
                  value={question}
                  onChange={(event) => setQuestion(event.target.value)}
                  onKeyDown={handleTextareaKeyDown}
                  placeholder="Ask a question about your documents..."
                  rows={1}
                  maxLength={2000}
                  disabled={sending}
                />

                <button
                  type="submit"
                  className="send-button"
                  disabled={sending || !question.trim()}
                >
                  {sending ? "..." : "Send"}
                </button>
              </form>

              <div className="composer-hint">
                Enter to send · Shift + Enter for a new line · Answers are
                grounded in your uploaded documents
              </div>
            </div>
          </>
        )}
      </section>

      <CreateConversationModal
        open={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onCreate={handleCreateConversation}
        creating={creatingConversation}
      />
    </div>
  );
}
