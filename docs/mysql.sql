

CREATE TABLE `tcc_lv1` (
  `tran_id` varchar(45) NOT NULL,
  `mod_id` varchar(45) NOT NULL,
  `status` varchar(45) NOT NULL,
  `create_time` varchar(45) NOT NULL,
  `update_time` varchar(45) NOT NULL,
  PRIMARY KEY (`tran_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `tcc_lv2` (
  `tran_id` varchar(45) NOT NULL,
  `mod_id` varchar(45) NOT NULL,
  `status` varchar(45) NOT NULL,
  `create_time` varchar(45) DEFAULT NULL,
  `update_time` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`tran_id`,`mod_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;




CREATE TABLE `order_tr` (
  `tran_id` varchar(45) NOT NULL,
  `order_id` varchar(45) NOT NULL,
  `status` varchar(45) NOT NULL,
  PRIMARY KEY (`tran_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


CREATE TABLE `order` (
  `order_id` varchar(45) NOT NULL,
  `franchiser_id` varchar(45) NOT NULL,
  `product_id` varchar(45) NOT NULL,
  `product_amount` varchar(45) NOT NULL,
  `status` varchar(45) NOT NULL,
  `tran_id` varchar(45) NOT NULL,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `tr_id_UNIQUE` (`tran_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `warehouse` (
  `franchiser_id` varchar(45) NOT NULL,
  `product_id` varchar(45) NOT NULL,
  `product_amount` varchar(45) NOT NULL,
  `last_tr_id` varchar(45) NOT NULL,
  PRIMARY KEY (`product_id`,`franchiser_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `warehouse_tr` (
  `tran_id` varchar(45) NOT NULL,
  `order_id` varchar(45) NOT NULL,
  `franchiser_id` varchar(45) NOT NULL,
  `product_id` varchar(45) NOT NULL,
  `product_amount` varchar(45) NOT NULL,
  `status` varchar(45) NOT NULL,
  PRIMARY KEY (`tran_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
