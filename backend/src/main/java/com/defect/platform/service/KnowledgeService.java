package com.defect.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.defect.platform.common.PageResult;
import com.defect.platform.dto.KnowledgeDTO;
import com.defect.platform.entity.Knowledge;
import com.defect.platform.vo.KnowledgeVO;
import com.defect.platform.vo.TagVO;

import java.util.List;

/**
 * 知识库服务：CRUD、检索、标签聚合、从缺陷一键沉淀
 */
public interface KnowledgeService extends IService<Knowledge> {

    KnowledgeVO create(KnowledgeDTO dto);

    KnowledgeVO update(Long id, KnowledgeDTO dto);

    void delete(Long id);

    KnowledgeVO get(Long id);

    PageResult<KnowledgeVO> list(long current, long size, String keyword, String type, String tag);

    /** 从已解决缺陷一键沉淀为知识（自动带入类型/根因/方案） */
    KnowledgeVO precipitate(Long defectId, KnowledgeDTO dto);

    /** 标签聚合（去重计数，用于标签云） */
    List<TagVO> tags();
}
