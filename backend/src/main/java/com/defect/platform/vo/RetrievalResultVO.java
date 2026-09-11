package com.defect.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识检索结果（含实际生效的检索方式，便于前端与验收时判断降级情况）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResultVO {

    /** 实际生效的检索方式：VECTOR / FULLTEXT / KEYWORD / NONE */
    private String mode;

    private List<RetrievalHitVO> hits;
}
