import React, { useState } from 'react';
import attachmentService from '../../services/attachmentService';

export default function MessageBubble({ message, isOwnMessage, isOutgoing, isGroup, onRetry }) {
  const isMine = isOwnMessage !== undefined ? isOwnMessage : Boolean(isOutgoing);
  const [lightboxUrl, setLightboxUrl] = useState(null);

  const formatTime = (isoString) => {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  // System message notice
  if (message.messageType === 'SYSTEM') {
    return (
      <div className="text-center my-2">
        <span
          className="badge rounded-pill bg-light text-muted border px-3 py-1 shadow-sm"
          style={{ fontSize: '0.75rem', fontWeight: 500 }}
        >
          {message.content}
        </span>
      </div>
    );
  }

  const renderStatus = () => {
    if (!isMine) return null;

    switch (message.status) {
      case 'SENDING':
        return (
          <i
            className="bi bi-clock text-muted ms-1"
            title="Sending..."
            style={{ fontSize: '0.72rem' }}
          ></i>
        );
      case 'FAILED':
        return (
          <button
            className="btn btn-link btn-sm p-0 text-danger ms-1 text-decoration-none d-inline-flex align-items-center gap-1"
            onClick={() => onRetry && onRetry(message.clientMessageId)}
            title="Failed to send. Click to retry."
            style={{ fontSize: '0.72rem' }}
          >
            <i className="bi bi-exclamation-circle-fill"></i> Retry
          </button>
        );
      case 'READ':
        return (
          <i
            className="bi bi-check-all ms-1 text-info"
            title="Read"
            style={{ fontSize: '0.85rem', color: '#53bdeb' }}
          ></i>
        );
      case 'DELIVERED':
        return (
          <i
            className="bi bi-check-all ms-1 text-muted"
            title="Delivered"
            style={{ fontSize: '0.85rem' }}
          ></i>
        );
      case 'SENT':
      default:
        return (
          <i
            className="bi bi-check ms-1 text-muted"
            title="Sent"
            style={{ fontSize: '0.85rem' }}
          ></i>
        );
    }
  };

  const senderDisplayName = message.senderFullName || message.senderUsername;

  const renderAttachments = () => {
    if (!message.attachments || message.attachments.length === 0) return null;

    return (
      <div className="message-attachments mb-2 d-flex flex-column gap-2">
        {message.attachments.map((att) => {
          const downloadUrl = att.storageUrl.startsWith('http')
            ? att.storageUrl
            : attachmentService.getAttachmentUrl(att.publicId);

          if (att.fileType === 'IMAGE') {
            const previewUrl = att.thumbnailUrl || downloadUrl;
            return (
              <div key={att.publicId} className="position-relative">
                <img
                  src={previewUrl}
                  alt={att.fileName}
                  className="rounded img-fluid cursor-pointer"
                  style={{ maxHeight: '240px', objectFit: 'cover', cursor: 'pointer' }}
                  onClick={() => setLightboxUrl(downloadUrl)}
                />
              </div>
            );
          }

          if (att.fileType === 'VIDEO') {
            return (
              <div key={att.publicId} className="rounded overflow-hidden">
                <video
                  controls
                  preload="metadata"
                  className="w-100 rounded"
                  style={{ maxHeight: '240px' }}
                >
                  <source src={downloadUrl} type={att.mimeType || 'video/mp4'} />
                  Your browser does not support the video tag.
                </video>
              </div>
            );
          }

          if (att.fileType === 'AUDIO') {
            return (
              <div key={att.publicId} className="p-1">
                <div className="small text-muted mb-1 text-truncate" style={{ maxWidth: '250px' }}>
                  <i className="bi bi-music-note-beamed me-1"></i>{att.fileName}
                </div>
                <audio controls preload="none" className="w-100" style={{ height: '36px' }}>
                  <source src={downloadUrl} type={att.mimeType || 'audio/mpeg'} />
                  Your browser does not support the audio tag.
                </audio>
              </div>
            );
          }

          // DOCUMENT default
          return (
            <div
              key={att.publicId}
              className="d-flex align-items-center justify-content-between p-2 rounded bg-light border text-dark"
              style={{ minWidth: '220px' }}
            >
              <div className="d-flex align-items-center gap-2 text-truncate me-2">
                <i className="bi bi-file-earmark-text fs-4 text-primary"></i>
                <div className="text-truncate">
                  <div className="small fw-semibold text-truncate" title={att.fileName}>
                    {att.fileName}
                  </div>
                  <div className="text-muted" style={{ fontSize: '0.72rem' }}>
                    {formatBytes(att.fileSize)}
                  </div>
                </div>
              </div>
              <a
                href={downloadUrl}
                download={att.fileName}
                target="_blank"
                rel="noreferrer"
                className="btn btn-outline-primary btn-sm rounded-circle p-1 d-flex align-items-center justify-content-center"
                style={{ width: '32px', height: '32px' }}
                title="Download"
              >
                <i className="bi bi-download"></i>
              </a>
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <>
      <div className={`d-flex ${isMine ? 'justify-content-end' : 'justify-content-start'} mb-2`}>
        <div
          className={`message-bubble ${
            isMine ? 'message-bubble-outgoing' : 'message-bubble-incoming'
          } shadow-sm`}
          style={{ maxWidth: '75%' }}
        >
          {isGroup && !isMine && senderDisplayName && (
            <div className="fw-bold mb-1" style={{ color: '#128C7E', fontSize: '0.78rem' }}>
              {senderDisplayName}
            </div>
          )}

          {renderAttachments()}

          {message.content && <div className="message-content">{message.content}</div>}

          <div className="message-meta d-flex align-items-center justify-content-end gap-1 mt-1">
            <span className="message-time">{formatTime(message.createdAt)}</span>
            {renderStatus()}
          </div>
        </div>
      </div>

      {/* Lightbox Modal */}
      {lightboxUrl && (
        <div
          className="modal fade show d-block"
          tabIndex="-1"
          style={{ backgroundColor: 'rgba(0,0,0,0.85)', zIndex: 1055 }}
          onClick={() => setLightboxUrl(null)}
        >
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content bg-transparent border-0 text-center">
              <div className="position-relative d-inline-block">
                <button
                  type="button"
                  className="btn btn-dark btn-sm rounded-circle position-absolute top-0 end-0 m-2 shadow"
                  onClick={() => setLightboxUrl(null)}
                  aria-label="Close"
                >
                  <i className="bi bi-x-lg"></i>
                </button>
                <img
                  src={lightboxUrl}
                  alt="Attachment preview"
                  className="img-fluid rounded shadow-lg"
                  style={{ maxHeight: '85vh', objectFit: 'contain' }}
                />
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
