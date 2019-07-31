CREATE TABLE `tb_wallet_transfer` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `transfer_no` varchar(32) NOT NULL COMMENT '交易号',
  `withdraw_order_no` varchar(256) NOT NULL DEFAULT ' ' COMMENT '客户端提币订单号',
  `transfer_type` tinyint(2) DEFAULT '-1' COMMENT '类型 1 申请手续费 2 客户端充币 3 客户端提币 4 提币到主地址 5 转到冷地址',
  `coin_type` varchar(32) NOT NULL DEFAULT '' COMMENT '币类型 BTC USDT ETH ',
  `address` varchar(256) DEFAULT '' COMMENT '用户子地址',
  `from_address` varchar(256) DEFAULT '' COMMENT '转出地址',
  `to_address` varchar(256) DEFAULT '' COMMENT '转入地址',
  `qty` decimal(32,16) DEFAULT NULL COMMENT '提币数量',
  `tx_id` varchar(200) DEFAULT '' COMMENT '交易hash',
  `tx_fee` decimal(32,16) DEFAULT NULL COMMENT '转出手续费',
  `status` tinyint(4) NOT NULL DEFAULT '-1' COMMENT '状态 0 失败 1 已产生交易hash，并进入txPool 2 离开txPool 3 已确认',
  `confirm_time` timestamp NULL DEFAULT NULL COMMENT '确认时间',
  `gas_price` decimal(32,16) DEFAULT '0.0000000000000000' COMMENT 'gasPrice',
  `gas_limit` decimal(32,16) DEFAULT '0.0000000000000000' COMMENT 'gasLimit',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `transfer_no_idx` (`transfer_no`) USING BTREE,
  KEY `order_no_idx` (`withdraw_order_no`) USING BTREE,
  KEY `trans_address_idx` (`address`) USING BTREE,
  KEY `tx_id_idx` (`tx_id`) USING BTREE,
  KEY `from_address_idx` (`from_address`) USING BTREE,
  KEY `to_address_idx` (`to_address`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COMMENT='钱包交易记录表';

CREATE TABLE `tb_wallet_assets` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `appid` varchar(256) DEFAULT '' COMMENT 'appid',
  `user_id` varchar(64) DEFAULT '' COMMENT '用户ID',
  `coin_type` varchar(32) NOT NULL COMMENT '币种  ETH BTC USDT',
  `address` varchar(256) NOT NULL COMMENT '钱包地址',
  `total_account` decimal(32,16) DEFAULT '0.0000000000000000' COMMENT '总资产',
  `version` int(11) DEFAULT '0' COMMENT '版本号',
  `create_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `assets_address_idx` (`address`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包资产表';


CREATE TABLE `tb_wallet_assets_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `appid` varchar(256) DEFAULT '' COMMENT 'appid',
  `user_id` varchar(64) DEFAULT '' COMMENT '用户ID',
  `order_no` varchar(64) NOT NULL COMMENT '流水号',
  `coin_type` varchar(32) NOT NULL COMMENT '币种  ETH BTC USDT',
  `transfer_type` tinyint(2) NOT NULL COMMENT '交易类型 1 充币 2 提币 3 买币 4 买币 5划转',
  `address` varchar(256) NOT NULL COMMENT '钱包地址',
  `account` decimal(32,16) DEFAULT '0.0000000000000000' COMMENT '金额',
  `create_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no_idx` (`order_no`) USING BTREE,
  KEY `address_idx` (`address`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包资产流水表';