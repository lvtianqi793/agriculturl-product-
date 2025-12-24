package com.ltqtest.springbootquickstart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ltqtest.springbootquickstart.entity.ExpertUserChatRecord;

@Repository
public interface ExpertUserChatRecordRepository extends JpaRepository<ExpertUserChatRecord, Integer> {

    List<ExpertUserChatRecord> findByExpertIdAndUserId(Integer expert_Id, Integer user_Id);

    Optional<ExpertUserChatRecord> findByEuChatId(Long euChatId);
    
}
