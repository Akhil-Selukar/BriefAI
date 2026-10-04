import { useEffect, useRef, useState } from "react";

import {
  deleteDocument,
  getDocuments,
  processDocument,
  uploadDocument,
} from "../api/documentApi";

import { getApiErrorMessage } from "../api/apiError";

const MAX_FILE_SIZE = 10 * 1024 * 1024;

const ALLOWED_EXTENSIONS = [".pdf", ".doc", ".docx"];

function formatBytes(bytes) {
  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(value) {
  if (!value) {
    return "";
  }

  return new Intl.DateTimeFormat("en", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(value));
}

function getFileType(document) {
  const name = document.originalName?.toLowerCase() || "";

  if (name.endsWith(".pdf")) {
    return "PDF";
  }

  if (name.endsWith(".docx")) {
    return "DOCX";
  }

  if (name.endsWith(".doc")) {
    return "DOC";
  }

  return "Document";
}

export default function DocumentsPage() {
  const fileInputRef = useRef(null);

  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);

  const [processingId, setProcessingId] = useState(null);

  const [deletingId, setDeletingId] = useState(null);

  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadDocuments = async () => {
    try {
      setError("");

      const data = await getDocuments();

      setDocuments(data);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDocuments();
  }, []);

  const handleFileSelected = async (event) => {
    const file = event.target.files?.[0];

    // Allows selecting the same file again later.
    event.target.value = "";

    if (!file) {
      return;
    }

    setError("");
    setMessage("");

    const lowerName = file.name.toLowerCase();

    const validExtension = ALLOWED_EXTENSIONS.some((extension) =>
      lowerName.endsWith(extension),
    );

    if (!validExtension) {
      setError("Only PDF, DOC, and DOCX files are supported.");
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      setError("Document size must not exceed 10 MB.");
      return;
    }

    try {
      setUploading(true);

      const uploaded = await uploadDocument(file);

      setDocuments((current) => [uploaded, ...current]);

      setMessage(`${uploaded.originalName} uploaded successfully.`);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setUploading(false);
    }
  };

  const handleProcess = async (documentId) => {
    setError("");
    setMessage("");

    try {
      setProcessingId(documentId);

      const updated = await processDocument(documentId);

      setDocuments((current) =>
        current.map((document) =>
          document.id === documentId ? updated : document,
        ),
      );

      setMessage(`${updated.originalName} is ready for questions.`);
    } catch (err) {
      setError(getApiErrorMessage(err));

      // Refresh because backend may have changed
      // the document status to FAILED.
      await loadDocuments();
    } finally {
      setProcessingId(null);
    }
  };

  const handleDelete = async (document) => {
    const confirmed = window.confirm(`Delete "${document.originalName}"?`);

    if (!confirmed) {
      return;
    }

    setError("");
    setMessage("");

    try {
      setDeletingId(document.id);

      await deleteDocument(document.id);

      setDocuments((current) =>
        current.filter((item) => item.id !== document.id),
      );

      setMessage("Document deleted.");
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setDeletingId(null);
    }
  };

  const readyCount = documents.filter(
    (document) => document.status === "READY",
  ).length;

  return (
    <div className="page-container">
      <header className="page-header">
        <div>
          <div className="eyebrow">KNOWLEDGE BASE</div>

          <h1>Your documents</h1>

          <p>
            Upload documents, process them for semantic search, and ask grounded
            questions with BriefAI.
          </p>
        </div>

        <button
          className="primary-button upload-button"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
        >
          {uploading ? "Uploading..." : "+ Upload document"}
        </button>

        <input
          ref={fileInputRef}
          className="hidden-file-input"
          type="file"
          accept=".pdf,.doc,.docx"
          onChange={handleFileSelected}
        />
      </header>

      <section className="document-stats">
        <div className="stat-card">
          <span className="stat-label">Documents</span>

          <strong>{documents.length}</strong>
        </div>

        <div className="stat-card">
          <span className="stat-label">Ready for chat</span>

          <strong>{readyCount}</strong>
        </div>

        <div className="stat-card">
          <span className="stat-label">Upload limit</span>

          <strong>{documents.length} / 5</strong>
        </div>
      </section>

      {error && <div className="alert alert-error page-alert">{error}</div>}

      {message && (
        <div className="alert alert-success page-alert">{message}</div>
      )}

      <section className="documents-panel">
        <div className="panel-header">
          <div>
            <h2>Document library</h2>

            <p>PDF, DOC and DOCX · Maximum 10 MB each</p>
          </div>
        </div>

        {loading ? (
          <div className="empty-state">
            <div className="loading-circle" />

            <h3>Loading documents</h3>

            <p>Fetching your document library...</p>
          </div>
        ) : documents.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">
              <svg
                xmlns="http://w3.org"
                width="48"
                height="48"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z" />
                <path d="M14 2v4a2 2 0 0 0 2 2h4" />
                <path d="M10 9H8" />
                <path d="M16 13H8" />
                <path d="M16 17H8" />
              </svg>
            </div>

            <h3>No documents yet</h3>

            <p>
              Upload your first document to start building your knowledge base.
            </p>

            <button
              className="secondary-button"
              onClick={() => fileInputRef.current?.click()}
            >
              Upload your first document
            </button>
          </div>
        ) : (
          <div className="document-list">
            {documents.map((document) => {
              const processing = processingId === document.id;

              const deleting = deletingId === document.id;

              return (
                <article className="document-row" key={document.id}>
                  <div className="file-icon">{getFileType(document)}</div>

                  <div className="document-info">
                    <div className="document-title-row">
                      <h3>{document.originalName}</h3>

                      <span
                        className={`status-badge status-${document.status.toLowerCase()}`}
                      >
                        <span className="status-dot" />

                        {processing ? "PROCESSING" : document.status}
                      </span>
                    </div>

                    <div className="document-meta">
                      <span>{getFileType(document)}</span>

                      <span>•</span>

                      <span>{formatBytes(document.sizeBytes)}</span>

                      {document.pageCount != null && (
                        <>
                          <span>•</span>

                          <span>
                            {document.pageCount}{" "}
                            {document.pageCount === 1 ? "page" : "pages"}
                          </span>
                        </>
                      )}

                      <span>•</span>

                      <span>{formatDate(document.createdAt)}</span>
                    </div>
                  </div>

                  <div className="document-actions">
                    {document.status === "UPLOADED" && (
                      <button
                        className="secondary-button"
                        disabled={processing || deleting}
                        onClick={() => handleProcess(document.id)}
                      >
                        {processing ? "Processing..." : "Process"}
                      </button>
                    )}

                    {document.status === "READY" && (
                      <span className="ready-label">Ready to chat</span>
                    )}

                    {document.status === "FAILED" && (
                      <span className="failed-label">Processing failed</span>
                    )}

                    <button
                      className="danger-button"
                      disabled={processing || deleting}
                      onClick={() => handleDelete(document)}
                    >
                      {deleting ? "Deleting..." : "Delete"}
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
