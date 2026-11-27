import React, { useState } from 'react';
import attachmentService from '../../services/attachmentService';

const EMOJI_OPTIONS = ['👍', '❤️', '😂', '😮', '😢', '🙏'];

export default function MessageBubble({
  message,
  isOwnMessage,
  isOutgoing,
  isGroup,
  onRetry,
  onReply,
  onReact,
  onRemoveReaction,
  onEdit,
  onDelete,
  onForward,
  onPin,
  currentUser
}) {
  const isMine = isOwnMessage !== undefined ? isOwnMessage : Boolean(isOutgoing);
  const [lightboxUrl, setLightboxUrl] = useState(null);
  const [showMenu, setShowMenu] = useState(false);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(message.content || '');

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
        return <i className="bi bi-clock text-muted ms-1" title="Sending..." style={{ fontSize: '0.72rem' }}></i>;
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
        return <i className="bi bi-check-all ms-1 text-info" title="Read" style={{ fontSize: '0.85rem', color: '#53bdeb' }}></i>;
      case 'DELIVERED':
        return <i className="bi bi-check-all ms-1 text-muted" title="Delivered" style={{ fontSize: '0.85rem' }}></i>;
      case 'SENT':
      default:
        return <i className="bi bi-check ms-1 text-muted" title="Sent" style={{ fontSize: '0.85rem' }}></i>;
    }
  };

  const handleEditSubmit = (e) => {
    e.preventDefault();
    if (editContent.trim() && editContent.trim() !== message.content) {
      onEdit && onEdit(message.publicId, editContent.trim());
    }
    setIsEditing(false);
  };

  const senderDisplayName = message.senderFullName || message.senderUsername;

  return (
    <>
      <div
        className={`d-flex ${isMine ? 'justify-content-end' : 'justify-content-start'} mb-2 position-relative`}
        onMouseLeave={() => {
          setShowMenu(false);
          setShowEmojiPicker(false);
        }}
      >
        <div
          className={`message-bubble ${
            isMine ? 'message-bubble-outgoing' : 'message-bubble-incoming'
          } shadow-sm position-relative group`}
          style={{ maxWidth: '75%', minWidth: '120px' }}
        >
          {/* Group sender name */}
          {isGroup && !isMine && senderDisplayName && (
            <div className="fw-bold mb-1" style={{ color: '#128C7E', fontSize: '0.78rem' }}>
              {senderDisplayName}
            </div>
          )}

          {/* Forwarded tag */}
          {message.isForwarded && (
            <div className="text-muted fst-italic mb-1 d-flex align-items-center gap-1" style={{ fontSize: '0.72rem' }}>
              <i className="bi bi-reply-fill" style={{ transform: 'scaleX(-1)' }}></i> Forwarded
            </div>
          )}

          {/* Reply snippet preview */}
          {message.replyToMessage && (
            <div
              className="p-2 mb-2 rounded bg-black bg-opacity-10 border-start border-4 border-success text-truncate"
              style={{ fontSize: '0.78rem' }}
            >
              <div className="fw-bold text-success text-truncate">
                {message.replyToMessage.senderFullName || message.replyToMessage.senderUsername}
              </div>
              <div className="text-muted text-truncate">{message.replyToMessage.content}</div>
            </div>
          )}

          {/* Pinned badge */}
          {message.isPinned && (
            <div className="badge bg-warning text-dark mb-1 py-1 px-2 d-inline-flex align-items-center gap-1" style={{ fontSize: '0.68rem' }}>
              <i className="bi bi-pin-angle-fill"></i> Pinned
            </div>
          )}

          {/* Attachments rendering */}
          {message.attachments && message.attachments.length > 0 && (
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
                        className="rounded img-fluid"
                        style={{ maxHeight: '240px', objectFit: 'cover', cursor: 'pointer' }}
                        onClick={() => setLightboxUrl(downloadUrl)}
                      />
                    </div>
                  );
                }

                if (att.fileType === 'VIDEO') {
                  return (
                    <div key={att.publicId} className="rounded overflow-hidden">
                      <video controls preload="metadata" className="w-100 rounded" style={{ maxHeight: '240px' }}>
                        <source src={downloadUrl} type={att.mimeType || 'video/mp4'} />
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
                      </audio>
                    </div>
                  );
                }

                return (
                  <div key={att.publicId} className="d-flex align-items-center justify-content-between p-2 rounded bg-light border text-dark" style={{ minWidth: '220px' }}>
                    <div className="d-flex align-items-center gap-2 text-truncate me-2">
                      <i className="bi bi-file-earmark-text fs-4 text-primary"></i>
                      <div className="text-truncate">
                        <div className="small fw-semibold text-truncate" title={att.fileName}>{att.fileName}</div>
                        <div className="text-muted" style={{ fontSize: '0.72rem' }}>{formatBytes(att.fileSize)}</div>
                      </div>
                    </div>
                    <a href={downloadUrl} download={att.fileName} target="_blank" rel="noreferrer" title="Download" className="btn btn-outline-primary btn-sm rounded-circle p-1 d-flex align-items-center justify-content-center" style={{ width: '32px', height: '32px' }}>
                      <i className="bi bi-download"></i>
                    </a>
                  </div>
                );
              })}
            </div>
          )}

          {/* Message Content or inline edit form */}
          {isEditing ? (
            <form onSubmit={handleEditSubmit} className="my-1">
              <input
                type="text"
                className="form-control form-control-sm mb-1"
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                autoFocus
              />
              <div className="d-flex gap-1 justify-content-end">
                <button type="button" className="btn btn-light btn-sm py-0 px-2" style={{ fontSize: '0.72rem' }} onClick={() => setIsEditing(false)}>Cancel</button>
                <button type="submit" className="btn btn-success btn-sm py-0 px-2" style={{ fontSize: '0.72rem' }}>Save</button>
              </div>
            </form>
          ) : (
            message.content && (
              <div className={`message-content ${message.deletedForEveryone ? 'fst-italic text-muted' : ''}`}>
                {message.content}
              </div>
            )
          )}

          {/* Reactions bar */}
          {message.reactions && message.reactions.length > 0 && (
            <div className="d-flex flex-wrap gap-1 mt-1">
              {Object.entries(
                message.reactions.reduce((acc, r) => {
                  acc[r.emoji] = (acc[r.emoji] || 0) + 1;
                  return acc;
                }, {})
              ).map(([emoji, count]) => {
                const isMyReaction = message.reactions.some(
                  (r) => r.emoji === emoji && r.username === currentUser?.username
                );
                return (
                  <span
                    key={emoji}
                    onClick={() => {
                      if (isMyReaction) {
                        onRemoveReaction && onRemoveReaction(message.publicId);
                      } else {
                        onReact && onReact(message.publicId, emoji);
                      }
                    }}
                    className={`badge rounded-pill border ${
                      isMyReaction ? 'bg-primary text-white' : 'bg-light text-dark'
                    } py-1 px-2 cursor-pointer shadow-sm`}
                    style={{ fontSize: '0.75rem', cursor: 'pointer' }}
                    title={isMyReaction ? 'Click to remove' : 'Click to react'}
                  >
                    {emoji} {count > 1 ? count : ''}
                  </span>
                );
              })}
            </div>
          )}

          {/* Metadata: time, edited tag, status ticks, action menu toggle */}
          <div className="message-meta d-flex align-items-center justify-content-end gap-1 mt-1">
            {message.edited && !message.deletedForEveryone && (
              <span className="text-muted fst-italic me-1" style={{ fontSize: '0.68rem' }}>(edited)</span>
            )}
            <span className="message-time">{formatTime(message.createdAt)}</span>
            {renderStatus()}

            {/* Context menu trigger */}
            {!message.deletedForEveryone && (
              <button
                type="button"
                className="btn btn-link text-secondary p-0 ms-1"
                style={{ fontSize: '0.8rem', opacity: 0.7 }}
                onClick={() => setShowMenu((prev) => !prev)}
                title="Options"
              >
                <i className="bi bi-chevron-down"></i>
              </button>
            )}
          </div>

          {/* Context Menu Dropdown */}
          {showMenu && (
            <div
              className="position-absolute bg-white shadow rounded border py-1 z-3"
              style={{
                top: '100%',
                [isMine ? 'right' : 'left']: '0',
                minWidth: '150px',
                zIndex: 1050
              }}
            >
              <button
                className="dropdown-item py-1 px-3 d-flex align-items-center gap-2 small"
                onClick={() => {
                  setShowEmojiPicker((prev) => !prev);
                }}
              >
                <i className="bi bi-emoji-smile"></i> React
              </button>
              <button
                className="dropdown-item py-1 px-3 d-flex align-items-center gap-2 small"
                onClick={() => {
                  setShowMenu(false);
                  onReply && onReply(message);
                }}
              >
                <i className="bi bi-reply"></i> Reply
              </button>
              <button
                className="dropdown-item py-1 px-3 d-flex align-items-center gap-2 small"
                onClick={() => {
                  setShowMenu(false);
                  onForward && onForward(message);
                }}
              >
                <i className="bi bi-arrow-right-short"></i> Forward
              </button>
              <button
                className="dropdown-item py-1 px-3 d-flex align-items-center gap-2 small"
                onClick={() => {
                  setShowMenu(false);
                  onPin && onPin(message.publicId, !message.isPinned);
                }}
              >
                <i className="bi bi-pin-angle"></i> {message.isPinned ? 'Unpin' : 'Pin'}
              </button>
              {isMine && (
                <button
                  className="dropdown-item py-1 px-3 d-flex align-items-center gap-2 small"
                  onClick={() => {
                    setShowMenu(false);
                    setIsEditing(true);
                  }}
                >
                  <i className="bi bi-pencil"></i> Edit
                </button>
              )}
              <hr className="my-1" />
              <button
                className="dropdown-item py-1 px-3 d-flex align-items-center gap-2 small text-danger"
                onClick={() => {
                  setShowMenu(false);
                  onDelete && onDelete(message.publicId, false);
                }}
              >
                <i className="bi bi-trash"></i> Delete for me
              </button>
              {isMine && (
                <button
                  className="dropdown-item py-1 px-3 d-flex align-items-center gap-2 small text-danger"
                  onClick={() => {
                    setShowMenu(false);
                    onDelete && onDelete(message.publicId, true);
                  }}
                >
                  <i className="bi bi-trash-fill"></i> Delete for everyone
                </button>
              )}
            </div>
          )}

          {/* Quick Reaction Picker */}
          {showEmojiPicker && (
            <div
              className="position-absolute bg-white rounded-pill shadow-lg border p-1 d-flex gap-2 z-3"
              style={{
                bottom: '100%',
                [isMine ? 'right' : 'left']: '0',
                marginBottom: '4px',
                zIndex: 1055
              }}
            >
              {EMOJI_OPTIONS.map((emoji) => (
                <button
                  key={emoji}
                  type="button"
                  className="btn btn-link p-1 text-decoration-none"
                  style={{ fontSize: '1.2rem', lineHeight: 1 }}
                  onClick={() => {
                    onReact && onReact(message.publicId, emoji);
                    setShowEmojiPicker(false);
                    setShowMenu(false);
                  }}
                >
                  {emoji}
                </button>
              ))}
            </div>
          )}
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
