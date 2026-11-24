import React, { useState, useEffect, useCallback } from 'react';
import Navbar from '../components/common/Navbar';
import ConversationList from '../components/chat/ConversationList';
import ChatArea from '../components/chat/ChatArea';
import NewConversationModal from '../components/chat/NewConversationModal';
import { conversationService } from '../services/conversationService';

export default function HomePage() {
  const [conversations, setConversations] = useState([]);
  const [loadingList, setLoadingList] = useState(true);
  const [activeFilter, setActiveFilter] = useState('all');

  const [selectedConversationId, setSelectedConversationId] = useState(null);
  const [activeConversation, setActiveConversation] = useState(null);
  const [loadingActive, setLoadingActive] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  const [showNewChatModal, setShowNewChatModal] = useState(false);

  const fetchConversations = useCallback(async () => {
    try {
      setLoadingList(true);
      const params = {
        page: 0,
        size: 50,
        sort: 'lastMessageAt,desc'
      };

      if (activeFilter === 'archived') {
        params.archived = true;
      } else if (activeFilter === 'unread') {
        params.unread = true;
      } else {
        params.archived = false;
      }

      const data = await conversationService.getConversations(params);
      setConversations(data?.content || []);
    } catch {
      // ignore
    } finally {
      setLoadingList(false);
    }
  }, [activeFilter]);

  useEffect(() => {
    fetchConversations();
  }, [fetchConversations]);

  const loadConversationDetails = async (publicId) => {
    try {
      setLoadingActive(true);
      setSelectedConversationId(publicId);
      const data = await conversationService.getConversation(publicId);
      setActiveConversation(data);
    } catch {
      setActiveConversation(null);
    } finally {
      setLoadingActive(false);
    }
  };

  const handleSelectConversation = (publicId) => {
    loadConversationDetails(publicId);
  };

  const handleBackToConversations = () => {
    setSelectedConversationId(null);
    setActiveConversation(null);
  };

  const handleConversationCreated = (newConv) => {
    fetchConversations();
    if (newConv?.publicId) {
      loadConversationDetails(newConv.publicId);
    }
  };

  const handleToggleArchive = async (publicId, archived) => {
    try {
      setActionLoading(true);
      await conversationService.archiveConversation(publicId, archived);
      if (activeConversation?.publicId === publicId) {
        setActiveConversation((prev) => ({ ...prev, archived }));
      }
      fetchConversations();
    } catch {
      // ignore
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleMute = async (publicId, muted) => {
    try {
      setActionLoading(true);
      await conversationService.muteConversation(publicId, muted);
      if (activeConversation?.publicId === publicId) {
        setActiveConversation((prev) => ({ ...prev, muted }));
      }
      fetchConversations();
    } catch {
      // ignore
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="min-vh-100 d-flex flex-column overflow-hidden">
      <Navbar />

      <div className="chat-container d-flex flex-grow-1">
        {/* Left Sidebar: visible on desktop, or on mobile when no conversation is open */}
        <div className={`h-100 ${selectedConversationId ? 'd-none d-md-flex' : 'd-flex w-100 w-md-auto'}`}>
          <ConversationList
            conversations={conversations}
            selectedId={selectedConversationId}
            onSelectConversation={handleSelectConversation}
            onOpenNewChatModal={() => setShowNewChatModal(true)}
            activeFilter={activeFilter}
            onChangeFilter={setActiveFilter}
            loading={loadingList}
          />
        </div>

        {/* Right Chat Area: visible on desktop, or on mobile when a conversation is open */}
        <div className={`h-100 flex-grow-1 ${selectedConversationId ? 'd-flex w-100' : 'd-none d-md-flex'}`}>
          <ChatArea
            conversation={activeConversation}
            onBack={handleBackToConversations}
            onToggleArchive={handleToggleArchive}
            onToggleMute={handleToggleMute}
            loadingAction={actionLoading || loadingActive}
          />
        </div>
      </div>

      {/* New Chat Modal */}
      <NewConversationModal
        show={showNewChatModal}
        onClose={() => setShowNewChatModal(false)}
        onConversationSelected={handleConversationCreated}
      />
    </div>
  );
}
