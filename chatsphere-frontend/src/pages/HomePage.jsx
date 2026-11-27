import React, { useState, useEffect, useCallback } from 'react';
import Navbar from '../components/common/Navbar';
import ConversationList from '../components/chat/ConversationList';
import ChatArea from '../components/chat/ChatArea';
import NewConversationModal from '../components/chat/NewConversationModal';
import NewGroupModal from '../components/chat/NewGroupModal';
import { conversationService } from '../services/conversationService';
import { useWebSocket } from '../hooks/useWebSocket';

export default function HomePage() {
  const [conversations, setConversations] = useState([]);
  const [loadingList, setLoadingList] = useState(true);
  const [activeFilter, setActiveFilter] = useState('all');

  const [selectedConversationId, setSelectedConversationId] = useState(null);
  const [activeConversation, setActiveConversation] = useState(null);
  const [loadingActive, setLoadingActive] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  const [showNewChatModal, setShowNewChatModal] = useState(false);
  const [showNewGroupModal, setShowNewGroupModal] = useState(false);

  const webSocket = useWebSocket();

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

      // Clear unread count locally when opened
      setConversations((prev) =>
        prev.map((c) =>
          c.publicId === publicId ? { ...c, unreadCount: 0 } : c
        )
      );
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

  const handleGroupCreated = (newGroup) => {
    fetchConversations();
    if (newGroup?.publicId) {
      loadConversationDetails(newGroup.publicId);
    }
  };

  const handleGroupUpdated = (updatedGroup) => {
    fetchConversations();
    if (activeConversation?.publicId === updatedGroup.publicId) {
      setActiveConversation((prev) => ({
        ...prev,
        title: updatedGroup.title,
        imageUrl: updatedGroup.imageUrl,
        participants: updatedGroup.members
      }));
    }
  };

  const handleLeaveGroup = () => {
    handleBackToConversations();
    fetchConversations();
  };

  // Live sidebar update when a message is sent or received
  const handleMessageUpdate = useCallback(
    (msg) => {
      if (!msg) return;
      const targetConvId = msg.conversationPublicId;

      setConversations((prev) => {
        const convIndex = prev.findIndex((c) => c.publicId === targetConvId);
        if (convIndex === -1) {
          // If not in the current list, refresh list
          fetchConversations();
          return prev;
        }

        const existing = prev[convIndex];
        const isCurrentActive = selectedConversationId === targetConvId;
        const updated = {
          ...existing,
          lastMessagePreview: msg.content,
          lastMessageAt: msg.createdAt || new Date().toISOString(),
          unreadCount: isCurrentActive ? 0 : (existing.unreadCount || 0) + 1
        };

        // Move the updated conversation to the top
        const newList = [...prev];
        newList.splice(convIndex, 1);
        return [updated, ...newList];
      });
    },
    [selectedConversationId, fetchConversations]
  );

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
            onOpenNewGroupModal={() => setShowNewGroupModal(true)}
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
            webSocket={webSocket}
            onMessageReceived={handleMessageUpdate}
            onToggleArchive={handleToggleArchive}
            onToggleMute={handleToggleMute}
            onGroupUpdated={handleGroupUpdated}
            onLeaveGroup={handleLeaveGroup}
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

      {/* New Group Modal */}
      <NewGroupModal
        show={showNewGroupModal}
        onClose={() => setShowNewGroupModal(false)}
        onGroupCreated={handleGroupCreated}
      />
    </div>
  );
}
