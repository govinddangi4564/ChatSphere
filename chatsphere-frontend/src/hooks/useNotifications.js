import { useState, useEffect, useCallback } from 'react';
import { notificationService } from '../services/notificationService';
import { websocketService } from '../services/websocketService';
import { useAuth } from './useAuth';

export function useNotifications() {
  const { isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState('all'); // 'all' | 'unread'

  const fetchUnreadCount = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const count = await notificationService.getUnreadCount();
      setUnreadCount(count);
    } catch {
      // ignore
    }
  }, [isAuthenticated]);

  const fetchNotifications = useCallback(async (isReadFilter) => {
    if (!isAuthenticated) return;
    try {
      setLoading(true);
      const params = {
        page: 0,
        size: 30,
        sort: 'createdAt,desc'
      };
      if (isReadFilter === false || filter === 'unread') {
        params.read = false;
      }
      const data = await notificationService.getNotifications(params);
      setNotifications(data?.content || []);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated, filter]);

  // Initial load and unread count fetch
  useEffect(() => {
    if (isAuthenticated) {
      fetchUnreadCount();
      fetchNotifications();
    }
  }, [isAuthenticated, fetchUnreadCount, fetchNotifications]);

  // Subscribe to real-time /user/queue/notifications over WebSocket
  useEffect(() => {
    if (!isAuthenticated) return;

    const unsubscribe = websocketService.subscribe('/user/queue/notifications', (newNotif) => {
      if (newNotif) {
        setNotifications((prev) => [newNotif, ...prev.filter((n) => n.publicId !== newNotif.publicId)]);
        setUnreadCount((c) => c + 1);
      }
    });

    return () => {
      if (unsubscribe) unsubscribe();
    };
  }, [isAuthenticated]);

  const markAsRead = async (publicId) => {
    try {
      await notificationService.markAsRead(publicId);
      setNotifications((prev) =>
        prev.map((n) => (n.publicId === publicId ? { ...n, isRead: true } : n))
      );
      setUnreadCount((c) => Math.max(0, c - 1));
    } catch {
      // ignore
    }
  };

  const markAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch {
      // ignore
    }
  };

  return {
    notifications,
    unreadCount,
    loading,
    filter,
    setFilter,
    fetchNotifications,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead
  };
}
