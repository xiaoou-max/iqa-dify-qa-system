package com.frml.api.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "messages")
@Data
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; //序号

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name="conversation_id",nullable = false)
    private String conversationId;

    @Column(name="message_id",nullable = false)
    private String messageId;

    @Column(name="create_time",nullable = false)
    private Date createTime;

    @Column(nullable = false)
    private String question;

    @Column(name="answer_stime")
    private Date answerStartTime;

    private String answer;

    @Column(name="answer_etime")
    private Date answerEndTime;
}
