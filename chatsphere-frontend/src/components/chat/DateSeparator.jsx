import React from 'react';

export default function DateSeparator({ dateStr }) {
  const formatDateLabel = (isoString) => {
    if (!isoString) return '';
    const date = new Date(isoString);
    const now = new Date();

    const isToday =
      date.getDate() === now.getDate() &&
      date.getMonth() === now.getMonth() &&
      date.getFullYear() === now.getFullYear();

    if (isToday) return 'Today';

    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    const isYesterday =
      date.getDate() === yesterday.getDate() &&
      date.getMonth() === yesterday.getMonth() &&
      date.getFullYear() === yesterday.getFullYear();

    if (isYesterday) return 'Yesterday';

    return date.toLocaleDateString([], {
      month: 'short',
      day: 'numeric',
      year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
    });
  };

  return (
    <div className="text-center my-3">
      <span className="badge rounded-pill bg-light text-muted border px-3 py-1 shadow-sm date-badge">
        {formatDateLabel(dateStr)}
      </span>
    </div>
  );
}
