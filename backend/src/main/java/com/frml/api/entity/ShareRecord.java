package com.frml.api.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "share_records")
@Data
public class ShareRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; //序号

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String code; //分享记录的编码，具有唯一性

    @Column(name="create_time",nullable = false)
    private Date createTime;

    @OneToMany(mappedBy = "shareRecord", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ShareMessageRelationship> messageRelationships;
}
