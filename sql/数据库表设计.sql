CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户信息的唯一标识序号',
    name VARCHAR(50) NOT NULL COMMENT '用户的姓名',
	code VARCHAR(100) UNIQUE NOT NULL COMMENT '用户的唯一编码'
	account VARCHAR(50) UNIQUE NOT NULL COMMENT '用户的登录账号',
	password VARCHAR(200) NOT NULL COMMENT '用户的登录密码'
) COMMENT '存储用户基本信息';



CREATE TABLE messages (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '会话消息的唯一标识序号',
    user_id INT NOT NULL COMMENT '关联的用户的序号',
	conversation_id VARCHAR(200) NOT NULL COMMENT '会话id，同一个会话可以包含多条对话记录',
	message_id VARCHAR(200) NOT NULL COMMENT '对话记录id，记录AI应用的对话记录id',
    create_time DATETIME NOT NULL COMMENT '创建时间',
	question VARCHAR(400) NOT NULL COMMENT '提问内容',
	answer_stime DATETIME COMMENT '回答开始时间',
	answer text COMMENT '回答内容',
	answer_etime DATETIME COMMENT '回答结束时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
	UNIQUE KEY unique_message_id (message_id)
) COMMENT '存储会话消息';



CREATE TABLE share_records (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '分享记录的唯一标识序号',
	user_id INT NOT NULL COMMENT '关联的用户的序号',
    code VARCHAR(50) UNIQUE NOT NULL COMMENT '分享记录的编码，具有唯一性',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
) COMMENT '存储分享记录';

CREATE TABLE share_message_relationship (
	id INT PRIMARY KEY AUTO_INCREMENT COMMENT '关系的唯一标识序号',
    share_record_id INT NOT NULL COMMENT '关联的分享记录序号',
    message_id INT NOT NULL COMMENT '关联的会话消息序号',    
    FOREIGN KEY (share_record_id) REFERENCES share_records(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE ON UPDATE CASCADE
) COMMENT '存储分享记录与会话消息的多对多关系';
    


