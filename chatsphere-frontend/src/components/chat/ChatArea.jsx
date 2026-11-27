import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useChatMessages } from '../../hooks/useChatMessages';
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
  loadingAction
}) => {
  const { user } = useAuth();
  const [inputText, setInputText] = useState('');
  const [showGroupInfo, setShowGroupInfo] = useState(false);
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
        <MessageBubble
          key={msg.clientMessageId || msg.publicId || `msg-${idx}`}
          message={msg}
          isOwnMessage={isOwn}
          isOutgoing={isOwn}
          isGroup={isGroup}
          onRetry={retryMessage}
        />
      );
    });

    return elements;
  };

  // Scroll to bottom on initial load or new outgoing messages
  useEffect(() => {
    if (!loadingMore && !isPrependingRef.current) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages.length, loadingMore]);

  // Adjust scroll position after prepending older messages
  useEffect(() => {
    if (isPrependingRef.current && messagesContainerRef.current) {
      const newScrollHeight = messagesContainerRef.current.scrollHeight;
      const scrollDiff = newScrollHeight - prevScrollHeightRef.current;
      messagesContainerRef.current.scrollTop += scrollDiff;
      isPrependingRef.current = false;
    }
  }, [messages]);

  // Upward infinite scroll handler
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

    // Send typing event
    sendTypingIndicator(true);

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    typingTimeoutRef.current = setTimeout(() => {
      sendTypingIndicator(false);
    }, 2500);
  };

  const handleSend = (e) => {
    e.preventDefault();
    const trimmed = inputText.trim();
    if (!trimmed) return;

    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
    sendTypingIndicator(false);

    sendMessage(trimmed);
    setInputText('');
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
    <div className="d-flex flex-column h-100 chat-area bg-light">
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
                  className="rounded-circle w-100 h-100"
                  style={{ objectFit: 'cover' }}
                />
              ) : isGroup ? (
                <i className="bi bi-people-fill"></i>
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
            <h6 className="mb-0 fw-semibold text-truncate" style={{ maxWidth: '240px' }}>
              {partnerName}
            </h6>
            <small className="text-muted">
              {isGroup ? (
                groupSubtitle
              ) : partnerTyping ? (
                <span className="text-success fw-semibold">typing...</span>
              ) : isOnline ? (
                <span className="text-success">online</span>
              ) : partnerUsername ? (
                partnerUsername
              ) : (
                'offline'
              )}
            </small>
          </div>
        </div>

        <div className="d-flex align-items-center text-secondary">
          {isGroup && (
            <button
              className="btn btn-sm btn-link text-secondary p-1 me-1"
              title="Group Info"
              onClick={() => setShowGroupInfo(true)}
            >
              <i className="bi bi-info-circle fs-5"></i>
            </button>
          )}
          {onToggleMute && (
            <button
              className={`btn btn-sm btn-link p-1 me-1 ${conversation.muted ? 'text-danger' : 'text-secondary'}`}
              onClick={() => onToggleMute(conversation.publicId, !conversation.muted)}
              disabled={loadingAction}
              title={conversation.muted ? 'Unmute' : 'Mute'}
            >
              <i className={`bi ${conversation.muted ? 'bi-bell-slash-fill' : 'bi-bell'}`}></i>
            </button>
          )}
          {onToggleArchive && (
            <button
              className={`btn btn-sm btn-link p-1 me-1 ${conversation.archived ? 'text-primary' : 'text-secondary'}`}
              onClick={() => onToggleArchive(conversation.publicId, !conversation.archived)}
              disabled={loadingAction}
              title={conversation.archived ? 'Unarchive' : 'Archive'}
            >
              <i className={`bi ${conversation.archived ? 'bi-archive-fill' : 'bi-archive'}`}></i>
            </button>
          )}
          <button className="btn btn-sm btn-link text-secondary p-1" title="More options">
            <i className="bi bi-three-dots-vertical fs-5"></i>
          </button>
        </div>
      </div>

      {/* Messages Scroll Area */}
      <div
        ref={messagesContainerRef}
        onScroll={handleScroll}
        className="chat-messages-container flex-grow-1 overflow-auto p-3"
        style={{
          backgroundImage: 'radial-gradient(#e0e0e0 1px, transparent 1px)',
          backgroundSize: '20px 20px',
          backgroundColor: '#efeae2'
        }}
      >
        {loadingMore && (
          <div className="text-center my-2">
            <div className="spinner-border spinner-border-sm text-secondary" role="status">
              <span className="visually-hidden">Loading older messages...</span>
            </div>
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
        <form onSubmit={handleSend} className="d-flex align-items-center">
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
            disabled={!inputText.trim()}
            title="Send"
          >
            <i className="bi bi-send-fill text-white" style={{ fontSize: '1rem', marginLeft: '2px' }}></i>
          </button>
        </form>
      </div>

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
