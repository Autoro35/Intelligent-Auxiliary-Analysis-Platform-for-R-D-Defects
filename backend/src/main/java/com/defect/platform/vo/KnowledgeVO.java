package com.defect.platform.vo;

import cn.hutool.core.util.StrUtil;
import com.defect.platform.common.constant.DefectTypeEnum;
import com.defect.platform.entity.Knowledge;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库视图对象（含类型中文描述、创建人姓名、标签拆分列表）
 */
@Data
public class KnowledgeVO {

    private Long id;

    private String title;

    private Long defectId;

    private String type;

    private String typeDesc;

    private String rootCause;

    private String solution;

    /** 标签原文（逗号分隔） */
    private String tags;

    /** 标签拆分列表，便于前端渲染标签云 */
    private List<String> tagList;

    private Integer viewCount;

    private Long createBy;

    private String createByName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 实体转视图对象（创建人姓名由服务层填充）
     */
    public static KnowledgeVO from(Knowledge k) {
        KnowledgeVO vo = new KnowledgeVO();
        vo.setId(k.getId());
        vo.setTitle(k.getTitle());
        vo.setDefectId(k.getDefectId());
        vo.setType(k.getType());
        vo.setTypeDesc(DefectTypeEnum.descOf(k.getType()));
        vo.setRootCause(k.getRootCause());
        vo.setSolution(k.getSolution());
        vo.setTags(k.getTags());
        if (StrUtil.isNotBlank(k.getTags())) {
            vo.setTagList(Arrays.stream(k.getTags().split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList()));
        }
        vo.setViewCount(k.getViewCount());
        vo.setCreateBy(k.getCreateBy());
        vo.setCreateTime(k.getCreateTime());
        vo.setUpdateTime(k.getUpdateTime());
        return vo;
    }
}
