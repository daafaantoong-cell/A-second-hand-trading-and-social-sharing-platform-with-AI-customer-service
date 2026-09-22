package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报实体
 * 表: tb_report
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_report")
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 举报人ID */
    private Long reporterId;

    /** 目标类型: 1博客 2评论 3商家 */
    private Integer targetType;

    /** 目标ID */
    private Long targetId;

    /** 举报原因 */
    private String reason;

    /** 证据图URL,逗号分隔 */
    private String evidenceUrls;

    /** 状态: 0待处理 1已受理 2已驳回 3已处理 */
    private Integer status;

    /** 处理人ID */
    private Long handlerId;

    /** 处理备注 */
    private String handleRemark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
