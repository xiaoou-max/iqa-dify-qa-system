package com.frml.api.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "share_message_relationship")
@Data
public class ShareMessageRelationship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_record_id", nullable = false,
            foreignKey = @ForeignKey(foreignKeyDefinition =
                    "FOREIGN KEY (share_record_id) REFERENCES share_records(id) ON DELETE CASCADE ON UPDATE CASCADE"))
    private ShareRecord shareRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false,
            foreignKey = @ForeignKey(foreignKeyDefinition =
                    "FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE ON UPDATE CASCADE"))
    private Message message;
}
