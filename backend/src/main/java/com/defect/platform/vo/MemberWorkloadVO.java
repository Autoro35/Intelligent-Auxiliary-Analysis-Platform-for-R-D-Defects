package com.defect.platform.vo;

import lombok.Data;

/**
 * 成员工作量统计（按当前处理人聚合）
 */
@Data
public class MemberWorkloadVO {

    /** 用户 ID */
    private Long userId;

    /** 用户姓名（昵称优先，回退用户名） */
    private String name;

    /** 当前负责的缺陷总数 */
    private Long total;

    /** 已关闭数量 */
    private Long closed;

    /** 处理中数量 */
    private Long processing;

    /** 待复测数量 */
    private Long pendingRetest;

    /** 已解决数量（有解决时间） */
    private Long resolved;

    /** 累计重新打开次数 */
    private Long reopenCount;
}
