package com.defect.platform.vo;

import cn.hutool.core.bean.BeanUtil;
import com.defect.platform.common.constant.DefectPriorityEnum;
import com.defect.platform.common.constant.DefectSeverityEnum;
import com.defect.platform.common.constant.DefectStatusEnum;
import com.defect.platform.common.constant.DefectTypeEnum;
import com.defect.platform.entity.Defect;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 缺陷视图对象（含各枚举中文描述、提交人/处理人姓名）
 */
@Data
public class DefectVO {

    private Long id;
    private Long projectId;
    private String title;
    private String description;

    private String type;
    private String typeDesc;

    private String priority;
    private String priorityDesc;

    private String severity;
    private String severityDesc;

    private String status;
    private String statusDesc;

    private Long reporterId;
    private String reporterName;

    private Long assigneeId;
    private String assigneeName;

    private String module;
    private String environment;
    private String reproduceSteps;
    private String expectedResult;
    private String actualResult;
    private String solution;
    private String rootCause;

    private Integer reopenCount;
    private LocalDateTime resolvedTime;
    private LocalDateTime closedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 实体转视图对象（提交人/处理人姓名由服务层填充）
     */
    public static DefectVO from(Defect d) {
        DefectVO vo = new DefectVO();
        BeanUtil.copyProperties(d, vo);
        vo.setTypeDesc(DefectTypeEnum.descOf(d.getType()));
        vo.setPriorityDesc(DefectPriorityEnum.descOf(d.getPriority()));
        vo.setSeverityDesc(DefectSeverityEnum.descOf(d.getSeverity()));
        vo.setStatusDesc(DefectStatusEnum.descOf(d.getStatus()));
        return vo;
    }
}
