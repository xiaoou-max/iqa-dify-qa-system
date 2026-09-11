package com.frml.api.dao;

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
public interface UserRepository  extends JpaRepository<User, Long> {

    /**
     * 根据账户和密码查询用户
     * @param account 用户账号
     * @param password 用户密码
     * @return 匹配的用户对象，未找到返回null
     */
    User findByAccountAndPassword(String account, String password);

    User findByCode(String code);
}
