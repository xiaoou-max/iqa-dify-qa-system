package com.frml.api.dao;

import com.frml.api.entity.Message;
import com.frml.api.entity.ShareMessageRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Author: lixiang
 * @Description:
 * @Date: 6/16/016 18:05
 */
@Repository
@Transactional
public interface ShareMessageRelationshipRepository extends JpaRepository<ShareMessageRelationship, Long> {

}
