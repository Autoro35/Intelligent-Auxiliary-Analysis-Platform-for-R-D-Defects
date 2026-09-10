package com.defect.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.defect.platform.common.PageResult;
import com.defect.platform.dto.DefectCommentDTO;
import com.defect.platform.dto.DefectDTO;
import com.defect.platform.dto.DefectTransitionDTO;
import com.defect.platform.entity.Defect;
import com.defect.platform.vo.DefectCommentVO;
import com.defect.platform.vo.DefectLogVO;
import com.defect.platform.vo.DefectVO;

import java.util.List;

/**
 * 缺陷服务：CRUD、生命周期状态流转、评论、操作日志
 */
public interface DefectService extends IService<Defect> {

    DefectVO create(DefectDTO dto);

    DefectVO update(Long id, DefectDTO dto);

    void delete(Long id);

    DefectVO get(Long id);

    PageResult<DefectVO> list(long current, long size, Long projectId, String status,
                              String type, String priority, Long assigneeId, String keyword);

    DefectVO transition(Long id, DefectTransitionDTO dto);

    DefectCommentVO addComment(Long id, DefectCommentDTO dto);

    List<DefectCommentVO> listComments(Long id);

    List<DefectLogVO> listLogs(Long id);
}
