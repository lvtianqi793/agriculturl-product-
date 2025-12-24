package com.ltqtest.springbootquickstart.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Data
@Entity
@Table(name = "expert_user_chat_record", schema = "ltq_adep")
@Builder
public class ExpertUserChatRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "euc_id", nullable = false, updatable = false)
    private Long euChatId; // bigint类型
    
    @Column(name = "question", length = 1000, nullable = false)
    private String question; // 提问内容

    @Column(name = "answer", length = 2000, nullable = true)
    private String answer; // 回答内容
    
    @Column(name = "send_time", nullable = false)
    private LocalDateTime sendTime; // 提问时间
    
    @Column(name = "user_id", nullable = false)
    private Integer userId; // 所属用户id

    @Column(name = "expert_id", nullable = false)
    private Integer expertId; // 所属专家id

    public ExpertUserChatRecord() {
    }

    public ExpertUserChatRecord(Long euChatId, String question, String answer, LocalDateTime sendTime, Integer userId, Integer expertId) {
        this.euChatId = euChatId;
        this.question = question;
        this.answer = answer;
        this.sendTime = sendTime;
        this.userId = userId;
        this.expertId = expertId;
    }
}
