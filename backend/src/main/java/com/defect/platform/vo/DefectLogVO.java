package com.defect.platform.vo;

import com.defect.platform.common.constant.DefectActionEnum;
import com.defect.platform.common.constant.DefectStatusEnum;
import com.defect.platform.entity.DefectLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 缺陷操作日志视图对象
 */
@Data
public class DefectLogVO {

    private Long id;
    private Long defectId;
    private Long operatorId;
    private String operatorName;

    private String action;
    private String actionDesc;

    private String fromStatus;
    private String fromStatusDesc;

    private String toStatus;
    private String toStatusDesc;

    private String remark;
    private LocalDateTime createTime;

    /**
     * 实体转视图对象（操作人姓名由服务层填充）
     */
    public static DefectLogVO from(DefectLog l) {
        DefectLogVO vo = new DefectLogVO();
        vo.setId(l.getId());
        vo.setDefectId(l.getDefectId());
        vo.setOperatorId(l.getOperatorId());
        vo.setAction(l.getAction());
        vo.setActionDesc(DefectActionEnum.descOf(l.getAction()));
        vo.setFromStatus(l.getFromStatus());
        vo.setFromStatusDesc(DefectStatusEnum.descOf(l.getFromStatus()));
        vo.setToStatus(l.getToStatus());
        vo.setToStatusDesc(DefectStatusEnum.descOf(l.getToStatus()));
        vo.setRemark(l.getRemark());
        vo.setCreateTime(l.getCreateTime());
        return vo;
    }
}
