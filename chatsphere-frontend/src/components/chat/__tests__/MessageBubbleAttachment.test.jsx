import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import MessageBubble from '../MessageBubble';

describe('MessageBubble Attachment Rendering', () => {
  it('renders image attachment and opens lightbox on click', () => {
    const message = {
      publicId: 'msg-1',
      clientMessageId: 'cli-1',
      senderUsername: 'alice',
      messageType: 'IMAGE',
      content: 'Here is the photo',
      createdAt: new Date().toISOString(),
      attachments: [
        {
          publicId: 'att-1',
          fileName: 'landscape.jpg',
          fileType: 'IMAGE',
          mimeType: 'image/jpeg',
          fileSize: 1048576,
          storageUrl: '/api/attachments/landscape.jpg',
          thumbnailUrl: null
        }
      ]
    };

    render(<MessageBubble message={message} isOwnMessage={false} isGroup={false} />);

    const img = screen.getByAltText('landscape.jpg');
    expect(img).toBeInTheDocument();
    expect(screen.getByText('Here is the photo')).toBeInTheDocument();

    // Click to open lightbox
    fireEvent.click(img);
    expect(screen.getByAltText('Attachment preview')).toBeInTheDocument();
  });

  it('renders document attachment with download link and file size', () => {
    const message = {
      publicId: 'msg-2',
      clientMessageId: 'cli-2',
      senderUsername: 'bob',
      messageType: 'DOCUMENT',
      content: '',
      createdAt: new Date().toISOString(),
      attachments: [
        {
          publicId: 'att-2',
          fileName: 'specification.pdf',
          fileType: 'DOCUMENT',
          mimeType: 'application/pdf',
          fileSize: 2097152, // 2MB
          storageUrl: '/api/attachments/att-2',
          thumbnailUrl: null
        }
      ]
    };

    render(<MessageBubble message={message} isOwnMessage={true} isGroup={false} />);

    expect(screen.getByText('specification.pdf')).toBeInTheDocument();
    expect(screen.getByText('2 MB')).toBeInTheDocument();
    const downloadBtn = screen.getByTitle('Download');
    expect(downloadBtn).toHaveAttribute('download', 'specification.pdf');
  });

  it('renders video attachment', () => {
    const message = {
      publicId: 'msg-3',
      clientMessageId: 'cli-3',
      senderUsername: 'alice',
      messageType: 'VIDEO',
      content: '',
      createdAt: new Date().toISOString(),
      attachments: [
        {
          publicId: 'att-3',
          fileName: 'demo.mp4',
          fileType: 'VIDEO',
          mimeType: 'video/mp4',
          fileSize: 5242880,
          storageUrl: '/api/attachments/demo.mp4',
          thumbnailUrl: null
        }
      ]
    };

    const { container } = render(<MessageBubble message={message} isOwnMessage={false} isGroup={false} />);
    const video = container.querySelector('video');
    expect(video).toBeInTheDocument();
  });
});
