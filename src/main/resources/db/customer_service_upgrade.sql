-- ============================================================
-- 黑马点评 客服升级 DDL
-- 执行顺序: 1.扩展订单字段  2.创建举报表
-- ============================================================
select * from tb_user
-- ---------- 1. 扩展 tb_voucher_order 表 (退款相关字段) ----------
ALTER TABLE tb_voucher_order
    ADD COLUMN refund_reason       VARCHAR(255) NULL COMMENT '退款原因' AFTER refund_time,
    ADD COLUMN refund_apply_time  DATETIME     NULL COMMENT '申请退款时间' AFTER refund_reason,
    ADD COLUMN refund_remark       VARCHAR(255) NULL COMMENT '退款审核备注' AFTER refund_apply_time;

-- 校验状态枚举: 1未付 / 2已付 / 3已核销 / 4已取消 / 5退款中 / 6已退款
-- (退款流程: 已付/已核销 -> 5退款中 -> 6已退款 或 4驳回为已取消)


-- ---------- 2. 创建 tb_report 举报表 ----------
DROP TABLE IF EXISTS tb_report;
CREATE TABLE tb_report (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    reporter_id     BIGINT       NOT NULL                COMMENT '举报人ID',
    target_type     TINYINT      NOT NULL                COMMENT '目标类型: 1博客 2评论 3商家',
    target_id       BIGINT       NOT NULL                COMMENT '目标ID',
    reason          VARCHAR(500) NOT NULL                COMMENT '举报原因',
    evidence_urls   VARCHAR(1000) NULL                   COMMENT '证据图URL,逗号分隔',
    status          TINYINT      NOT NULL DEFAULT 0      COMMENT '0待处理 1已受理 2已驳回 3已处理',
    handler_id      BIGINT       NULL                    COMMENT '处理人ID',
    handle_remark   VARCHAR(500) NULL                    COMMENT '处理备注',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                          COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_reporter (reporter_id),
    KEY idx_target (target_type, target_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';
