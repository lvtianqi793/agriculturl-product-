package com.ltqtest.springbootquickstart.entity;

import jakarta.persistence.*;
import java.util.Date;

/**
 * 求购需求实体类
 */
import lombok.Data;

@Data
@Entity
@Table(name = "buy_request", schema = "ltq_adep")
public class BuyRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "buy_request_id")
    private Integer buyRequestId;
    
    @Column(name = "title", nullable = false, length = 200)
    private String title = "暂无标题";
    
    @Column(name = "content", nullable = false, length = 10000)
    private String content;
    
    @Column(name = "contact", length = 100)
    private String contact;
    
    @Column(name = "create_time")
    private Date createTime;
    
    // getter and setter
    public Integer getBuyRequestId() {
        return buyRequestId;
    }
    
    public void setBuyRequestId(Integer buyRequestId) {
        this.buyRequestId = buyRequestId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getContact() {
        return contact;
    }
    
    public void setContact(String contact) {
        this.contact = contact;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}