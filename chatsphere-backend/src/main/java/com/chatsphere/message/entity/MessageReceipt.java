package com.chatsphere.message.entity;

import com.chatsphere.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "message_receipts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_message_receipt_msg_recipient", columnNames = {"message_id", "recipient_id"})
}, indexes = {
        @Index(name = "idx_receipts_recipient", columnList = "recipient_id"),
        @Index(name = "idx_receipts_message", columnList = "message_id")
})
@Getter
@Setter
@NoArgsConstructor
public class MessageReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;
}
