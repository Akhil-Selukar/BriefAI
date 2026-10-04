import { useEffect, useRef, useState } from "react";

export default function CreateConversationModal({
  open,
  onClose,
  onCreate,
  creating = false,
}) {
  const [title, setTitle] = useState("");
  const inputRef = useRef(null);

  useEffect(() => {
    if (open) {
      setTitle("");

      setTimeout(() => {
        inputRef.current?.focus();
      }, 0);
    }
  }, [open]);

  if (!open) {
    return null;
  }

  const handleSubmit = (event) => {
    event.preventDefault();

    const cleanTitle = title.trim();

    if (!cleanTitle || cleanTitle.length > 200 || creating) {
      return;
    }

    onCreate(cleanTitle);
  };

  return (
    <div
      className="modal-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !creating) {
          onClose();
        }
      }}
    >
      <div
        className="conversation-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="new-conversation-title"
      >
        <div className="modal-header">
          <div>
            <span className="modal-eyebrow">NEW CHAT</span>

            <h2 id="new-conversation-title">Create conversation</h2>
          </div>

          <button
            type="button"
            className="modal-close"
            onClick={onClose}
            disabled={creating}
          >
            ×
          </button>
        </div>

        <p className="modal-description">
          Give this conversation a short title so you can find it later.
        </p>

        <form onSubmit={handleSubmit}>
          <label className="modal-label" htmlFor="conversation-title-input">
            Conversation title
          </label>

          <input
            ref={inputRef}
            id="conversation-title-input"
            type="text"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="e.g. Project architecture"
            maxLength={200}
            disabled={creating}
          />

          <div className="modal-character-count">{title.length} / 200</div>

          <div className="modal-actions">
            <button
              type="button"
              className="secondary-button"
              onClick={onClose}
              disabled={creating}
            >
              Cancel
            </button>

            <button
              type="submit"
              className="primary-button"
              disabled={!title.trim() || creating}
            >
              {creating ? "Creating..." : "Create conversation"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
