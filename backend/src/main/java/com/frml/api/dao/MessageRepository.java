package com.frml.api.dao;

import com.frml.api.entity.Message;
import com.frml.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: lixiang
 * @Description:
 * @Date: 6/16/016 18:05
 */
@Repository
@Transactional
public interface MessageRepository extends JpaRepository<Message, Long> {

    Message findByMessageId(String messageId);
}
