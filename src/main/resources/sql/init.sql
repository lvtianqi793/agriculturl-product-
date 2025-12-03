DROP TABLE IF EXISTS `shopping_cart`;
DROP TABLE IF EXISTS `purchase`;
DROP TABLE IF EXISTS `product_comment`;
DROP TABLE IF EXISTS `repayment_record`;
DROP TABLE IF EXISTS `repayment_plan`;
DROP TABLE IF EXISTS `approval_record`;
DROP TABLE IF EXISTS `user_address`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `loan_application`;
DROP TABLE IF EXISTS `expert_user_chat_record`;
DROP TABLE IF EXISTS `expert_appointment`;
DROP TABLE IF EXISTS `tb_user`;
DROP TABLE IF EXISTS `tb_news`;
DROP TABLE IF EXISTS `loan_status`;
DROP TABLE IF EXISTS `financial_product`;
DROP TABLE IF EXISTS `experts`;
DROP TABLE IF EXISTS `buy_request`;
DROP TABLE IF EXISTS `approver`;
DROP TABLE IF EXISTS `agriculture_knowledge`;


CREATE TABLE `agriculture_knowledge` (
                                         `id` int NOT NULL AUTO_INCREMENT COMMENT '唯一标识，主键自增',
                                         `title` varchar(255) DEFAULT NULL COMMENT '文章标题',
                                         `source` varchar(255) DEFAULT NULL COMMENT '文章来源',
                                         `url` varchar(255) DEFAULT NULL COMMENT '原文链接',
                                         `publish` datetime DEFAULT NULL COMMENT '文章发布日期',
                                         PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='农业知识文章表';

CREATE TABLE `approver` (
                            `approverId` int NOT NULL AUTO_INCREMENT COMMENT '审批人编号',
                            `approverName` varchar(255) DEFAULT NULL COMMENT '审批人名字',
                            `approverPhone` varchar(20) DEFAULT NULL COMMENT '审批人电话',
                            `approverEmail` varchar(50) DEFAULT NULL COMMENT '审批人邮箱',
                            PRIMARY KEY (`approverId`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `buy_request` (
                               `buy_request_id` int NOT NULL COMMENT '求购id',
                               `title` varchar(200) NOT NULL DEFAULT '暂无标题' COMMENT '求购标题',
                               `content` varchar(10000) NOT NULL COMMENT '求购说明内容',
                               `contact` varchar(100) DEFAULT NULL COMMENT '联系方式',
                               `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                               PRIMARY KEY (`buy_request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='求购需求表';

CREATE TABLE `experts` (
                           `expertId` int NOT NULL AUTO_INCREMENT,
                           `expertName` varchar(255) NOT NULL,
                           `field` text,
                           `expertDescription` text,
                           `expertImg` varchar(255) DEFAULT NULL,
                           `example` text,
                           `expertPhone` varchar(50) DEFAULT NULL,
                           `expertEmail` varchar(100) DEFAULT NULL,
                           PRIMARY KEY (`expertId`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `financial_product` (
                                     `fpId` int NOT NULL AUTO_INCREMENT,
                                     `fpName` varchar(255) NOT NULL COMMENT '金融产品名称',
                                     `fpDescription` text NOT NULL COMMENT '产品简介',
                                     `annualRate` float DEFAULT NULL COMMENT '利率',
                                     `tags` varchar(500) DEFAULT NULL COMMENT '标签信息，逗号分隔',
                                     `fpManagerName` varchar(50) DEFAULT NULL COMMENT '负责人名字',
                                     `maxAmount` int NOT NULL COMMENT '最大额度',
                                     `minAmount` int NOT NULL COMMENT '最小额度',
                                     `term` int DEFAULT NULL COMMENT '贷款期限（单位：月）',
                                     `fpManagerPhone` varchar(20) DEFAULT NULL COMMENT '管理人员电话',
                                     `fpManagerEmail` varchar(100) DEFAULT NULL COMMENT '管理人员邮箱',
                                     PRIMARY KEY (`fpId`),
                                     CONSTRAINT `financial_product_chk_1` CHECK ((`maxAmount` >= 0)),
                                     CONSTRAINT `financial_product_chk_2` CHECK ((`minAmount` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `loan_status` (
                               `status_id` int NOT NULL AUTO_INCREMENT,
                               `status_code` int NOT NULL,
                               `status_name` varchar(50) NOT NULL,
                               `description` varchar(255) DEFAULT NULL,
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (`status_id`),
                               UNIQUE KEY `status_code` (`status_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tb_news` (
                           `newsId` int NOT NULL AUTO_INCREMENT COMMENT '新闻的id',
                           `title` varchar(255) NOT NULL COMMENT '新闻标题',
                           `imgUrl` varchar(512) NOT NULL COMMENT '新闻封面图片的url',
                           `newsUrl` varchar(512) NOT NULL COMMENT '新闻的url',
                           PRIMARY KEY (`newsId`) COMMENT '以新闻id为主键，确保唯一性',
                           KEY `idx_title` (`title`) COMMENT '为标题标题创建索引，优化查询效率'
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='新闻轮播表，存储用于轮播展示的新闻信息';

CREATE TABLE `tb_user` (
                           `user_id` int NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识（自增）',
                           `username` varchar(50) NOT NULL COMMENT '登录用户名',
                           `password` varchar(100) NOT NULL COMMENT '加密后的密码（如MD5+盐值）',
                           `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名（农户/买家/专家姓名；银行工作人员姓名）',
                           `role_type` tinyint NOT NULL DEFAULT '1' COMMENT '角色类型：1-农户，2-买家，3-专家，4-银行工作人员，5-平台管理员',
                           `phone` varchar(20) DEFAULT NULL COMMENT '联系电话（用于登录验证、消息通知）',
                           `email` varchar(100) DEFAULT NULL COMMENT '邮箱（参考课程教师联系方式格式设计）',
                           `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号（农户/专家实名认证；银行工作人员资质校验）',
                           `status` tinyint DEFAULT '1' COMMENT '账号状态：1-正常，0-禁用（平台管理员管理）',
                           `login_status` tinyint DEFAULT '0' COMMENT '账号登录状态： 0-未登录 1-已登录',
                           `create_time` datetime DEFAULT NULL COMMENT '账号创建时间',
                           `update_time` datetime DEFAULT NULL COMMENT '账号更新时间',
                           `image_url` varchar(500) DEFAULT NULL COMMENT '用户头像',
                           `expert_id` int NOT NULL DEFAULT '0' COMMENT '用户对应的专家id，0为普通用户',
                           `approver_id` int NOT NULL DEFAULT '0' COMMENT '银行工作人员id',
                           PRIMARY KEY (`user_id`),
                           UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='存储所有角色的基础信息';

CREATE TABLE `expert_appointment` (
                                      `expert_appointment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约记录ID（自增主键）',
                                      `expert_id` int NOT NULL COMMENT '专家ID',
                                      `user_id` int NOT NULL COMMENT '当前用户ID',
                                      `date` date NOT NULL COMMENT '预约日期（YYYY-MM-DD）',
                                      `startTime` time DEFAULT NULL COMMENT '预约开始时间',
                                      `endTime` time DEFAULT NULL COMMENT '预约结束时间',
                                      `topic` varchar(255) DEFAULT NULL COMMENT '咨询主题',
                                      `status` varchar(10) DEFAULT NULL COMMENT '状态',
                                      `remark` text COMMENT '备注说明',
                                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `comment` text COMMENT '专家备注',
                                      `report` text COMMENT '咨询总结',
                                      PRIMARY KEY (`expert_appointment_id`),
                                      KEY `fk_appointment_expert` (`expert_id`),
                                      KEY `fk_appointment_user` (`user_id`),
                                      CONSTRAINT `fk_appointment_expert` FOREIGN KEY (`expert_id`) REFERENCES `experts` (`expertId`),
                                      CONSTRAINT `fk_appointment_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专家预约记录表';

CREATE TABLE `expert_user_chat_record` (
                                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
                                           `expert_id` int NOT NULL COMMENT '专家ID（关联 experts.id）',
                                           `user_id` int NOT NULL COMMENT '用户ID（可关联 users.id）',
                                           `sender` enum('user','expert') NOT NULL COMMENT '发送者身份',
                                           `content` text NOT NULL COMMENT '消息内容',
                                           `type` enum('text','image','file') DEFAULT 'text' COMMENT '消息类型',
                                           `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读（0未读，1已读）',
                                           `timestamp` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
                                           PRIMARY KEY (`id`),
                                           KEY `fk_expert` (`expert_id`),
                                           KEY `fk_user` (`user_id`),
                                           CONSTRAINT `fk_expert` FOREIGN KEY (`expert_id`) REFERENCES `experts` (`expertId`),
                                           CONSTRAINT `fk_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专家与用户的聊天记录';

CREATE TABLE `loan_application` (
                                    `applicationId` int NOT NULL AUTO_INCREMENT COMMENT '申请ID',
                                    `userId` int NOT NULL COMMENT '用户ID',
                                    `fpId` int NOT NULL COMMENT '产品ID',
                                    `amount` int NOT NULL COMMENT '贷款金额',
                                    `term` int NOT NULL COMMENT '贷款期限',
                                    `documents` varchar(1000) DEFAULT NULL COMMENT '文件信息',
                                    `status` int NOT NULL DEFAULT '1' COMMENT '申请状态',
                                    `applyTime` datetime NOT NULL COMMENT '申请时间',
                                    PRIMARY KEY (`applicationId`),
                                    KEY `idx_userId` (`userId`),
                                    KEY `idx_productId` (`fpId`),
                                    CONSTRAINT `fk_loan_application_productId` FOREIGN KEY (`fpId`) REFERENCES `financial_product` (`fpId`) ON DELETE CASCADE ON UPDATE CASCADE,
                                    CONSTRAINT `fk_loan_application_userId` FOREIGN KEY (`userId`) REFERENCES `tb_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='贷款申请表';

CREATE TABLE `product` (
                           `product_id` int NOT NULL AUTO_INCREMENT COMMENT '商品id',
                           `product_name` varchar(255) NOT NULL COMMENT '商品名称',
                           `price` double NOT NULL COMMENT '商品单价',
                           `producer` varchar(255) NOT NULL COMMENT '发售商',
                           `salesVolume` int NOT NULL COMMENT '销售量',
                           `productImg` varchar(512) NOT NULL COMMENT '商品封面url',
                           `surplus` int NOT NULL COMMENT '商品的剩余量',
                           `user_id` int DEFAULT NULL COMMENT '商品所属的农户id',
                           `total_volumn` int DEFAULT NULL COMMENT '总商品数量',
                           `status` int DEFAULT '1' COMMENT '1为已上架 2位已下架 3为缺货',
                           PRIMARY KEY (`product_id`),
                           KEY `product_tb_user_user_id_fk` (`user_id`),
                           CONSTRAINT `product_tb_user_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品表';

CREATE TABLE `user_address` (
                                `address_id` int NOT NULL AUTO_INCREMENT COMMENT '地址id',
                                `user_id` int NOT NULL COMMENT '该地址对应的用户',
                                `address_name` varchar(200) DEFAULT '暂无地址' COMMENT '用户详细地址',
                                PRIMARY KEY (`address_id`),
                                KEY `user_address_tb_user_user_id_fk` (`user_id`),
                                CONSTRAINT `user_address_tb_user_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户地址表';

CREATE TABLE `approval_record` (
                                   `recordId` int NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                   `applicationId` int NOT NULL COMMENT '关联的贷款申请ID',
                                   `approverId` int NOT NULL COMMENT '审批人ID',
                                   `decision` tinyint(1) NOT NULL COMMENT '审批通过/不通过',
                                   `opinion` varchar(500) NOT NULL COMMENT '审批意见',
                                   `approvalTime` datetime NOT NULL COMMENT '审批时间',
                                   PRIMARY KEY (`recordId`),
                                   KEY `idx_applicationId` (`applicationId`),
                                   KEY `fk_approval_approver` (`approverId`),
                                   CONSTRAINT `fk_approval_application` FOREIGN KEY (`applicationId`) REFERENCES `loan_application` (`applicationId`) ON DELETE CASCADE,
                                   CONSTRAINT `fk_approval_approver` FOREIGN KEY (`approverId`) REFERENCES `approver` (`approverId`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批意见子表';

CREATE TABLE `repayment_plan` (
                                  `planId` int NOT NULL AUTO_INCREMENT COMMENT '计划ID',
                                  `applicationId` int NOT NULL COMMENT '贷款申请ID',
                                  `due_date` date NOT NULL COMMENT '到期日期',
                                  `RemainingAmount` float NOT NULL COMMENT '剩余还款金额',
                                  `status` varchar(10) NOT NULL DEFAULT '未还' COMMENT '状态（未还/已还/逾期）',
                                  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`planId`),
                                  UNIQUE KEY `idx_application_installment` (`applicationId`),
                                  CONSTRAINT `repayment_plan_ibfk_1` FOREIGN KEY (`applicationId`) REFERENCES `loan_application` (`applicationId`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='还款计划表';

CREATE TABLE `repayment_record` (
                                    `recordId` int NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                    `applicationId` int NOT NULL COMMENT '贷款申请ID',
                                    `userId` int NOT NULL COMMENT '用户ID',
                                    `amount` float NOT NULL COMMENT '还款金额',
                                    `pay_date` date NOT NULL COMMENT '还款日期',
                                    `status` varchar(10) NOT NULL DEFAULT '已还' COMMENT '状态（已还/部分还款）',
                                    `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    PRIMARY KEY (`recordId`),
                                    KEY `idx_user_id` (`userId`),
                                    KEY `idx_application_id` (`applicationId`),
                                    CONSTRAINT `repayment_record_ibfk_1` FOREIGN KEY (`applicationId`) REFERENCES `loan_application` (`applicationId`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='还款记录表';

CREATE TABLE `product_comment` (
                                   `product_comment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论id',
                                   `content` varchar(1000) NOT NULL COMMENT '评论内容',
                                   `send_time` datetime NOT NULL COMMENT '评论时间',
                                   `user_id` int NOT NULL COMMENT '所属用户id',
                                   `product_id` int NOT NULL COMMENT '所属商品id',
                                   `comment_like_count` bigint NOT NULL DEFAULT '0' COMMENT '点赞次数（为空说明没人点赞）',
                                   `root_comment_id` bigint DEFAULT NULL COMMENT '父评论id（为空说明该评论没有父评论）',
                                   `to_comment_id` bigint DEFAULT NULL COMMENT '该评论回复评论的评论id',
                                   PRIMARY KEY (`product_comment_id`),
                                   KEY `product_id` (`product_id`),
                                   KEY `user_id` (`user_id`),
                                   CONSTRAINT `product_comment_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`) ON DELETE CASCADE,
                                   CONSTRAINT `product_comment_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品评论表';

CREATE TABLE `purchase` (
                            `purchase_id` int NOT NULL AUTO_INCREMENT COMMENT '购物车ID（自增主键）',
                            `product_id` int NOT NULL COMMENT '商品ID',
                            `user_id` int NOT NULL COMMENT '购买人的Id',
                            `amount` int NOT NULL COMMENT '商品数量',
                            `total_price` double NOT NULL COMMENT '商品金额',
                            `get_address` varchar(200) DEFAULT NULL COMMENT '收货地址',
                            `status` int DEFAULT '1' COMMENT '状态（农产品商品有7个状态：1.刚上架状态 2上架后被消费者置入购物车，待支付 3.消费者付款，待发货阶段 4.农户发货阶段 5消费者已收货 6.该订单被取消（买家取消或卖家取消）7.已退货（每一个商品加一个退货字段，是否支持退货））',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            PRIMARY KEY (`purchase_id`),
                            KEY `product_id` (`product_id`),
                            KEY `user_id` (`user_id`),
                            CONSTRAINT `purchase_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`) ON DELETE CASCADE ON UPDATE CASCADE,
                            CONSTRAINT `purchase_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购买记录表';

CREATE TABLE `shopping_cart` (
                                 `cart_id` int NOT NULL AUTO_INCREMENT COMMENT '购物车ID（自增主键）',
                                 `product_id` int NOT NULL COMMENT '商品ID',
                                 `user_id` int NOT NULL COMMENT '用户ID',
                                 `amount` int NOT NULL COMMENT '商品数量',
                                 `total_price` double NOT NULL COMMENT '商品金额',
                                 `get_address` varchar(200) DEFAULT NULL COMMENT '收货地址',
                                 `status` int DEFAULT '1' COMMENT '状态（1-未结算，2-已结算，3-已取消）',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 PRIMARY KEY (`cart_id`),
                                 KEY `product_id` (`product_id`),
                                 KEY `user_id` (`user_id`),
                                 CONSTRAINT `shopping_cart_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`) ON DELETE CASCADE ON UPDATE CASCADE,
                                 CONSTRAINT `shopping_cart_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车表';

insert into tb_news values (1,'11月5日，这场智慧农业盛会带你解锁“种地”新姿势','https://cn.bing.com/th?id=OVFT.tKWtDRIPS_-sUj0rLfHz-C&pid=News&w=424&h=223&c=14&rs=2&qlt=90&dpr=1.8','https://www.msn.cn/zh-cn/%E6%8A%80%E6%9C%AF/%E4%BA%BA%E5%B7%A5%E6%99%BA%E8%83%BD/11%E6%9C%885%E6%97%A5-%E8%BF%99%E5%9C%BA%E6%99%BA%E6%85%A7%E5%86%9C%E4%B8%9A%E7%9B%9B%E4%BC%9A%E5%B8%A6%E4%BD%A0%E8%A7%A3%E9%94%81-%E7%A7%8D%E5%9C%B0-%E6%96%B0%E5%A7%BF%E5%8A%BF/ar-AA1PBIjv?ocid=BingNewsSerp');

INSERT INTO financial_product (
    fpName, fpDescription, annualRate, tags,
    fpManagerName, maxAmount, minAmount, term,
    fpManagerPhone, fpManagerEmail
) VALUES
      (
          '个人消费贷',
          '用于日常消费的小额贷款，审批快、到账快',
          4.8,
          '个人,消费,小额,短期',
          '张三',
          50000,
          1000,
          '12',
          '13800138000',
          'zhang3@example.com'
      ),
      (
          '企业经营贷',
          '为中小企业提供的经营周转资金贷款，支持企业扩大生产',
          3.6,
          '企业,经营,大额,中长期',
          '李四',
          2000000,
          100000,
          '24',
          '13900139000',
          'li4@example.com'
      ),
      (
          '房贷专项贷',
          '用于购买商品房的专项贷款，利率较低，期限长',
          3.25,
          '房贷,专项,长期,大额',
          '王五',
          5000000,
          500000,
          '36',
          '13700137000',
          'wang5@example.com'
      ),
      (
          '信用周转贷',
          '凭个人信用申请的短期周转贷款，无需抵押',
          5.2,
          '信用,周转,短期,无抵押',
          '赵六',
          30000,
          5000,
          NULL,
          '13600136000',
          NULL
      );

INSERT INTO experts (expertName, field, expertDescription, expertImg, example, expertPhone, expertEmail) VALUES
                                                                                                             ('伪王小唐', '智慧农业,养殖', '阿巴阿巴', 'https://example.com/avatar1.jpg', '我是问题', '1111111111', '23301012@bjtu.edu.cn'),
                                                                                                             ('农业专家', '种植技术,病虫害防治', '多年农业技术经验', 'https://example.com/avatar2.jpg', '解决了大面积病虫害问题', '2222222222', 'expert@example.com');
select * from experts;



INSERT INTO approver (approverName, approverPhone, approverEmail) VALUES
                                                                      ('张三', '13800138000', 'zhangsan@example.com'),
                                                                      ('李四', '13900139000', 'lisi@example.com'),
                                                                      ('王五', '13700137000', 'wangwu@example.com'),
                                                                      ('赵六', '13600136000', 'zhaoliu@example.com'),
                                                                      ('孙七', '13500135000', NULL), -- 模拟无邮箱的情况
                                                                      ('周八', NULL, 'zhouba@example.com'), -- 模拟无电话的情况
                                                                      ('吴九', '13400134000', 'wujiu@example.com');

insert into agriculture_knowledge values (1,'从经验到科学，作物育种进化图鉴','科普宣传','https://www.moa.gov.cn/ztzl/zjyqwgz/kpxc/202510/t20251016_6478191.htm','2025-10-16 10:11');

INSERT INTO loan_status (status_code, status_name, description) VALUES
                                                                    (1, '已提交', '用户已提交贷款申请'),
                                                                    (2, '已打回', '贷款申请被退回'),
                                                                    (3, '已审核', '审核通过，银行发放贷款'),
                                                                    (4, '已还款', '贷款已全部还清'),
                                                                    (5, '已逾期', '贷款超出还款期未还清');