import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useChatMessages } from '../../hooks/useChatMessages';
import messageService from '../../services/messageService';
import attachmentService from '../../services/attachmentService';
import MessageBubble from './MessageBubble';
import DateSeparator from './DateSeparator';
import TypingIndicator from './TypingIndicator';
import GroupInfoModal from './GroupInfoModal';

const ChatArea = ({
  conversation,
  onBack,
  webSocket,
  onMessageReceived,
  onToggleArchive,
  onToggleMute,
  onGroupUpdated,
  onLeaveGroup,
  loadingAction,
  onConversationCleared
}) => {
  const { user } = useAuth();
  const [inputText, setInputText] = useState('');
  const [showGroupInfo, setShowGroupInfo] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadError, setUploadError] = useState(null);
  const [pendingAttachments, setPendingAttachments] = useState([]);
  const [replyingTo, setReplyingTo] = useState(null);

  // Pinned messages & search
  const [pinnedMessages, setPinnedMessages] = useState([]);
  const [showSearch, setShowSearch] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);

  // Forward dialog
  const [forwardingMsg, setForwardingMsg] = useState(null);
  const [targetConvId, setTargetConvId] = useState('');

  const fileInputRef = useRef(null);
  const abortControllerRef = useRef(null);
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const typingTimeoutRef = useRef(null);
  const prevScrollHeightRef = useRef(0);
  const isPrependingRef = useRef(false);

  const {
    messages,
    loading,
    loadingMore,
    hasMore,
    partnerTyping,
    loadMoreMessages,
    sendMessage,
    retryMessage,
    sendTypingIndicator
  } = useChatMessages(conversation?.publicId, user, webSocket, onMessageReceived);

  const isGroup = conversation?.type === 'GROUP' || conversation?.conversationType === 'GROUP';

  // Load pinned messages on conversation change
  useEffect(() => {
    if (conversation?.publicId) {
      messageService.getPinnedMessages(conversation.publicId)
        .then((pins) => setPinnedMessages(pins || []))
        .catch(() => setPinnedMessages([]));
    }
  }, [conversation?.publicId]);

  // Handle Search
  const handleSearch = async (e) => {
    e?.preventDefault();
    if (!searchQuery.trim() || !conversation?.publicId) return;

    setSearching(true);
    try {
      const res = await messageService.searchMessages({
        conversationId: conversation.publicId,
        query: searchQuery.trim(),
        page: 0,
        size: 20
      });
      setSearchResults(res?.content || []);
    } catch (err) {
      console.error('Search failed', err);
    } finally {
      setSearching(false);
    }
  };

  // Actions
  const handleEditMessage = async (msgPublicId, newContent) => {
    try {
      await messageService.editMessage(msgPublicId, newContent);
    } catch (err) {
      console.error('Failed to edit message', err);
    }
  };

  const handleDeleteMessage = async (msgPublicId, forEveryone) => {
    try {
      await messageService.deleteMessage(msgPublicId, forEveryone);
    } catch (err) {
      console.error('Failed to delete message', err);
    }
  };

  const handleReact = async (msgPublicId, emoji) => {
    try {
      await messageService.addReaction(msgPublicId, emoji);
    } catch (err) {
      console.error('Failed to add reaction', err);
    }
  };

  const handleRemoveReaction = async (msgPublicId) => {
    try {
      await messageService.removeReaction(msgPublicId);
    } catch (err) {
      console.error('Failed to remove reaction', err);
    }
  };

  const handlePin = async (msgPublicId, pin) => {
    try {
      if (pin) {
        await messageService.pinMessage(msgPublicId);
      } else {
        await messageService.unpinMessage(msgPublicId);
      }
      const updatedPins = await messageService.getPinnedMessages(conversation.publicId);
      setPinnedMessages(updatedPins || []);
    } catch (err) {
      console.error('Failed to toggle pin', err);
    }
  };

  const handleForwardSubmit = async () => {
    if (!forwardingMsg || !targetConvId.trim()) return;
    try {
      await messageService.forwardMessage(forwardingMsg.publicId, targetConvId.trim());
      setForwardingMsg(null);
      setTargetConvId('');
    } catch (err) {
      console.error('Failed to forward message', err);
    }
  };

  const handleDeleteConversationForMe = async () => {
    if (!conversation?.publicId) return;
    if (window.confirm('Are you sure you want to clear this chat history for yourself?')) {
      try {
        await messageService.deleteConversationForMe(conversation.publicId);
        if (onConversationCleared) onConversationCleared(conversation.publicId);
        window.location.reload();
      } catch (err) {
        console.error('Failed to clear conversation', err);
      }
    }
  };

  // Group messages with dates
  const renderMessages = () => {
    let lastDate = null;
    const elements = [];

    messages.forEach((msg, idx) => {
      const msgDate = new Date(msg.createdAt).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });

      if (msgDate !== lastDate) {
        elements.push(
          <DateSeparator key={`date-${msgDate}-${idx}`} dateString={msgDate} />
        );
        lastDate = msgDate;
      }

      const isOwn = msg.senderPublicId === user?.publicId || msg.senderUsername === user?.username;

      elements.push(
        <div id={`msg-${msg.publicId}`} key={msg.clientMessageId || msg.publicId || `msg-${idx}`}>
          <MessageBubble
            message={msg}
            isOwnMessage={isOwn}
            isOutgoing={isOwn}
            isGroup={isGroup}
            onRetry={retryMessage}
            onReply={(m) => setReplyingTo(m)}
            onReact={handleReact}
            onRemoveReaction={handleRemoveReaction}
            onEdit={handleEditMessage}
            onDelete={handleDeleteMessage}
            onForward={(m) => setForwardingMsg(m)}
            onPin={handlePin}
            currentUser={user}
          />
        </div>
      );
    });

    return elements;
  };

  useEffect(() => {
    if (!loadingMore && !isPrependingRef.current) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages.length, loadingMore]);

  useEffect(() => {
    if (isPrependingRef.current && messagesContainerRef.current) {
      const newScrollHeight = messagesContainerRef.current.scrollHeight;
      const scrollDiff = newScrollHeight - prevScrollHeightRef.current;
      messagesContainerRef.current.scrollTop += scrollDiff;
      isPrependingRef.current = false;
    }
  }, [messages]);

  const handleScroll = useCallback(() => {
    const container = messagesContainerRef.current;
    if (!container) return;

    if (container.scrollTop <= 60 && hasMore && !loadingMore && !loading) {
      isPrependingRef.current = true;
      prevScrollHeightRef.current = container.scrollHeight;
      loadMoreMessages();
    }
  }, [hasMore, loadingMore, loading, loadMoreMessages]);

  const handleInputChange = (e) => {
    const val = e.target.value;
    setInputText(val);

    sendTypingIndicator(true);

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    typingTimeoutRef.current = setTimeout(() => {
      sendTypingIndicator(false);
    }, 2500);
  };

  const handleFileSelect = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    e.target.value = '';
    setUploading(true);
    setUploadProgress(0);
    setUploadError(null);

    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      const res = await attachmentService.uploadAttachment(
        file,
        (percent) => setUploadProgress(percent),
        controller.signal
      );
      if (res) {
        setPendingAttachments((prev) => [...prev, res]);
      }
    } catch (err) {
      if (err.name !== 'CanceledError' && err.message !== 'canceled') {
        const msg = err.response?.data?.message || err.message || 'File upload failed';
        setUploadError(msg);
      }
    } finally {
      setUploading(false);
      abortControllerRef.current = null;
    }
  };

  const handleCancelUpload = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    setUploading(false);
    setUploadProgress(0);
    setUploadError(null);
  };

  const handleRemovePending = (publicId) => {
    setPendingAttachments((prev) => prev.filter((a) => a.publicId !== publicId));
  };

  const handleSend = (e) => {
    e.preventDefault();
    const trimmed = inputText.trim();
    if (!trimmed && pendingAttachments.length === 0) return;

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
    sendTypingIndicator(false);

    const attachmentIds = pendingAttachments.map((a) => a.publicId);
    const primaryType = pendingAttachments.length > 0 ? pendingAttachments[0].fileType : 'TEXT';

    sendMessage(
      trimmed,
      attachmentIds,
      primaryType,
      pendingAttachments,
      replyingTo
    );

    setInputText('');
    setPendingAttachments([]);
    setReplyingTo(null);
    setUploadError(null);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend(e);
    }
  };

  if (!conversation) {
    return (
      <div className="h-100 d-flex flex-column align-items-center justify-content-center bg-light text-muted p-4">
        <i className="bi bi-chat-square-text display-1 mb-3 text-secondary opacity-50"></i>
        <h4 className="fw-semibold">ChatSphere Web</h4>
        <p className="text-center text-muted" style={{ maxWidth: '420px' }}>
          Select a conversation from the sidebar or search for users to start messaging in real-time.
        </p>
      </div>
    );
  }

  const partner = conversation.otherParticipant || {};
  const partnerName = isGroup
    ? (conversation.title || 'Group')
    : (partner.fullName || partner.username || conversation.title || 'Chat');
  const partnerUsername = partner.username ? `@${partner.username}` : '';
  const isOnline = !isGroup && partner.isOnline;

  const groupSubtitle = partnerTyping ? (
    <span className="text-success fw-semibold">typing...</span>
  ) : conversation.participants ? (
    `${conversation.participants.length} members`
  ) : (
    'Click for group info'
  );

  return (
    <div className="d-flex flex-column h-100 chat-area bg-light position-relative">
      {/* Chat Header */}
      <div className="chat-header d-flex align-items-center justify-content-between px-3 py-2 bg-white border-bottom shadow-sm">
        <div
          className="d-flex align-items-center"
          style={{ cursor: isGroup ? 'pointer' : 'default' }}
          onClick={() => isGroup && setShowGroupInfo(true)}
        >
          {onBack && (
            <button
              className="btn btn-link text-dark p-0 me-2 d-md-none"
              onClick={(e) => {
                e.stopPropagation();
                onBack();
              }}
              aria-label="Back to conversations"
            >
              <i className="bi bi-arrow-left fs-4"></i>
            </button>
          )}
          <div className="position-relative me-3">
            <div
              className={`rounded-circle d-flex align-items-center justify-content-center text-white fw-bold ${
                isGroup ? 'bg-success' : 'bg-secondary'
              }`}
              style={{ width: '42px', height: '42px', fontSize: '1.1rem' }}
            >
              {conversation.imageUrl ? (
                <img
                  src={conversation.imageUrl}
                  alt={partnerName}
                  className="rounded-circle w-100 h-100 object-fit-cover"
                />
              ) : (
                partnerName.charAt(0).toUpperCase()
              )}
            </div>
            {isOnline && (
              <span
                className="position-absolute bottom-0 end-0 bg-success border border-white rounded-circle"
                style={{ width: '10px', height: '10px' }}
                title="Online"
              ></span>
            )}
          </div>
          <div>
            <h6 className="mb-0 fw-semibold text-dark">{partnerName}</h6>
            <small className="text-muted">
              {isGroup ? groupSubtitle : isOnline ? (
                <span className="text-success">online</span>
              ) : (
                partnerUsername
              )}
            </small>
          </div>
        </div>

        {/* Header Actions */}
        <div className="d-flex align-items-center gap-1">
          {isGroup && (
            <button
              type="button"
              className="btn btn-link text-secondary p-2"
              onClick={() => setShowGroupInfo(true)}
              title="Group Info"
            >
              <i className="bi bi-info-circle fs-5"></i>
            </button>
          )}
          <button
            type="button"
            className="btn btn-link text-secondary p-2"
            onClick={() => setShowSearch((prev) => !prev)}
            title="Search messages"
          >
            <i className="bi bi-search fs-5"></i>
          </button>
          <button
            type="button"
            className="btn btn-link text-secondary p-2"
            onClick={handleDeleteConversationForMe}
            title="Clear chat for me"
          >
            <i className="bi bi-trash fs-5"></i>
          </button>
          {onToggleArchive && (
            <button
              type="button"
              className="btn btn-link text-secondary p-2"
              onClick={() => onToggleArchive(conversation.publicId, !conversation.isArchived)}
              disabled={loadingAction}
              title={conversation.isArchived ? 'Unarchive chat' : 'Archive chat'}
            >
              <i className={`bi bi-archive${conversation.isArchived ? '-fill text-success' : ''} fs-5`}></i>
            </button>
          )}
          {onToggleMute && (
            <button
              type="button"
              className="btn btn-link text-secondary p-2"
              onClick={() => onToggleMute(conversation.publicId, !conversation.isMuted)}
              disabled={loadingAction}
              title={conversation.isMuted ? 'Unmute chat' : 'Mute chat'}
            >
              <i className={`bi bi-bell${conversation.isMuted ? '-slash-fill text-warning' : ''} fs-5`}></i>
            </button>
          )}
        </div>
      </div>

      {/* Pinned Messages Bar */}
      {pinnedMessages.length > 0 && (
        <div className="bg-warning-subtle border-bottom px-3 py-1 d-flex align-items-center justify-content-between" style={{ fontSize: '0.82rem' }}>
          <div className="d-flex align-items-center gap-2 text-truncate">
            <i className="bi bi-pin-angle-fill text-warning"></i>
            <span className="fw-semibold">Pinned:</span>
            <span
              className="text-truncate cursor-pointer text-dark"
              style={{ cursor: 'pointer', maxWidth: '350px' }}
              onClick={() => {
                const el = document.getElementById(`msg-${pinnedMessages[0].publicId}`);
                el?.scrollIntoView({ behavior: 'smooth' });
              }}
            >
              {pinnedMessages[0].content || '[Attachment]'}
            </span>
          </div>
          <button
            type="button"
            className="btn btn-link text-muted p-0"
            style={{ fontSize: '0.75rem' }}
            onClick={() => handlePin(pinnedMessages[0].publicId, false)}
            title="Unpin message"
          >
            Unpin
          </button>
        </div>
      )}

      {/* Search Bar Panel */}
      {showSearch && (
        <div className="p-2 bg-white border-bottom shadow-sm">
          <form onSubmit={handleSearch} className="d-flex align-items-center gap-2">
            <input
              type="text"
              className="form-control form-control-sm"
              placeholder="Search in conversation..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              autoFocus
            />
            <button type="submit" className="btn btn-primary btn-sm px-3" disabled={searching}>
              {searching ? '...' : 'Search'}
            </button>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={() => {
                setShowSearch(false);
                setSearchResults([]);
                setSearchQuery('');
              }}
            >
              Close
            </button>
          </form>

          {/* Search Results Drawer */}
          {searchResults.length > 0 && (
            <div className="mt-2 border-top pt-1" style={{ maxHeight: '150px', overflowY: 'auto' }}>
              <small className="text-muted fw-semibold">Found {searchResults.length} message(s):</small>
              {searchResults.map((sm) => (
                <div
                  key={sm.publicId}
                  className="p-1 border-bottom d-flex align-items-center justify-content-between cursor-pointer"
                  style={{ cursor: 'pointer', fontSize: '0.8rem' }}
                  onClick={() => {
                    const el = document.getElementById(`msg-${sm.publicId}`);
                    el?.scrollIntoView({ behavior: 'smooth' });
                  }}
                >
                  <span className="text-truncate" style={{ maxWidth: '300px' }}>{sm.content}</span>
                  <small className="text-muted">{new Date(sm.createdAt).toLocaleDateString()}</small>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Message History Container */}
      <div
        ref={messagesContainerRef}
        className="chat-messages-container flex-grow-1 p-3 overflow-auto"
        onScroll={handleScroll}
      >
        {loadingMore && (
          <div className="text-center my-2">
            <div className="spinner-border spinner-border-sm text-secondary" role="status"></div>
          </div>
        )}

        {loading ? (
          <div className="h-100 d-flex align-items-center justify-content-center">
            <div className="spinner-border text-success" role="status">
              <span className="visually-hidden">Loading messages...</span>
            </div>
          </div>
        ) : messages.length === 0 ? (
          <div className="h-100 d-flex flex-column align-items-center justify-content-center text-muted">
            <i className="bi bi-chat-dots fs-1 mb-2 text-secondary opacity-50"></i>
            <p className="mb-0">No messages yet.</p>
            <small>{isGroup ? 'Send a message to the group!' : 'Say hello to start the conversation!'}</small>
          </div>
        ) : (
          <>
            {renderMessages()}
            {partnerTyping && <TypingIndicator partnerName={partnerName} />}
            <div ref={messagesEndRef} />
          </>
        )}
      </div>

      {/* Message Input Bar */}
      <div className="chat-input-bar bg-white p-2 border-top">
        {/* Reply Preview Bar */}
        {replyingTo && (
          <div className="p-2 mb-2 bg-light border-start border-4 border-success rounded d-flex align-items-center justify-content-between">
            <div className="text-truncate me-2" style={{ fontSize: '0.8rem' }}>
              <span className="fw-bold text-success d-block">
                Replying to {replyingTo.senderFullName || replyingTo.senderUsername}
              </span>
              <span className="text-muted text-truncate">{replyingTo.content || '[Attachment]'}</span>
            </div>
            <button
              type="button"
              className="btn-close"
              style={{ fontSize: '0.65rem' }}
              onClick={() => setReplyingTo(null)}
              aria-label="Cancel reply"
            ></button>
          </div>
        )}

        {/* Upload Error Banner */}
        {uploadError && (
          <div className="alert alert-danger py-1 px-3 mb-2 d-flex align-items-center justify-content-between" style={{ fontSize: '0.85rem' }}>
            <span><i className="bi bi-exclamation-triangle-fill me-2"></i>{uploadError}</span>
            <button type="button" className="btn-close" style={{ fontSize: '0.65rem' }} onClick={() => setUploadError(null)}></button>
          </div>
        )}

        {/* Uploading Progress Bar */}
        {uploading && (
          <div className="p-2 mb-2 bg-light border rounded d-flex align-items-center gap-2">
            <div className="spinner-border spinner-border-sm text-success" role="status"></div>
            <div className="progress flex-grow-1" style={{ height: '8px' }}>
              <div
                className="progress-bar bg-success progress-bar-striped progress-bar-animated"
                role="progressbar"
                style={{ width: `${uploadProgress}%` }}
                aria-valuenow={uploadProgress}
                aria-valuemin="0"
                aria-valuemax="100"
              ></div>
            </div>
            <span className="small text-muted">{uploadProgress}%</span>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm py-0 px-2"
              onClick={handleCancelUpload}
              style={{ fontSize: '0.75rem' }}
            >
              Cancel
            </button>
          </div>
        )}

        {/* Pending Attachments List */}
        {pendingAttachments.length > 0 && (
          <div className="d-flex flex-wrap gap-2 mb-2 p-1">
            {pendingAttachments.map((att) => (
              <div
                key={att.publicId}
                className="badge bg-light text-dark border p-2 d-flex align-items-center gap-2 shadow-sm"
                style={{ fontSize: '0.8rem', fontWeight: 500 }}
              >
                <i
                  className={
                    att.fileType === 'IMAGE'
                      ? 'bi bi-file-earmark-image text-primary'
                      : att.fileType === 'VIDEO'
                      ? 'bi bi-file-earmark-play text-danger'
                      : att.fileType === 'AUDIO'
                      ? 'bi bi-file-earmark-music text-info'
                      : 'bi bi-file-earmark-text text-secondary'
                  }
                ></i>
                <span className="text-truncate" style={{ maxWidth: '150px' }} title={att.fileName}>
                  {att.fileName}
                </span>
                <button
                  type="button"
                  className="btn-close ms-1"
                  style={{ fontSize: '0.55rem' }}
                  onClick={() => handleRemovePending(att.publicId)}
                  aria-label="Remove attachment"
                ></button>
              </div>
            ))}
          </div>
        )}

        <form onSubmit={handleSend} className="d-flex align-items-center">
          <input
            type="file"
            ref={fileInputRef}
            className="d-none"
            onChange={handleFileSelect}
          />
          <button
            type="button"
            className="btn btn-link text-secondary p-2 me-1"
            title="Emoji"
          >
            <i className="bi bi-emoji-smile fs-5"></i>
          </button>
          <button
            type="button"
            className="btn btn-link text-secondary p-2 me-2"
            title="Attach file"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading}
          >
            <i className="bi bi-paperclip fs-5"></i>
          </button>
          <input
            type="text"
            className="form-control rounded-pill px-3 py-2 bg-light border-0 me-2"
            placeholder="Type a message..."
            value={inputText}
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
            disabled={!conversation}
            autoFocus
          />
          <button
            type="submit"
            className="btn btn-success rounded-circle d-flex align-items-center justify-content-center shadow-sm"
            style={{ width: '42px', height: '42px', flexShrink: 0 }}
            disabled={!inputText.trim() && pendingAttachments.length === 0}
            title="Send"
          >
            <i className="bi bi-send-fill text-white" style={{ fontSize: '1rem', marginLeft: '2px' }}></i>
          </button>
        </form>
      </div>

      {/* Forward Message Modal */}
      {forwardingMsg && (
        <div className="modal fade show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 1060 }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content shadow">
              <div className="modal-header">
                <h5 className="modal-title">Forward Message</h5>
                <button type="button" className="btn-close" onClick={() => setForwardingMsg(null)}></button>
              </div>
              <div className="modal-body">
                <div className="p-2 mb-3 bg-light rounded text-muted" style={{ fontSize: '0.85rem' }}>
                  {forwardingMsg.content || '[Attachment]'}
                </div>
                <label className="form-label small fw-semibold">Target Conversation Public ID</label>
                <input
                  type="text"
                  className="form-control form-control-sm"
                  placeholder="Enter recipient or group conversation ID..."
                  value={targetConvId}
                  onChange={(e) => setTargetConvId(e.target.value)}
                />
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary btn-sm" onClick={() => setForwardingMsg(null)}>Cancel</button>
                <button type="button" className="btn btn-success btn-sm" onClick={handleForwardSubmit} disabled={!targetConvId.trim()}>Forward</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Group Info Modal */}
      {isGroup && (
        <GroupInfoModal
          show={showGroupInfo}
          onClose={() => setShowGroupInfo(false)}
          groupPublicId={conversation.publicId}
          onGroupUpdated={(updated) => {
            if (onGroupUpdated) onGroupUpdated(updated);
          }}
          onLeaveGroup={(publicId) => {
            setShowGroupInfo(false);
            if (onLeaveGroup) onLeaveGroup(publicId);
          }}
        />
      )}
    </div>
  );
};

export default ChatArea;
