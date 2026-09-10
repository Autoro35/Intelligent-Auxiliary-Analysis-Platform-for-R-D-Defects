package com.defect.platform.service;

import com.defect.platform.vo.AttachmentVO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 附件服务：上传、下载、删除
 */
public interface AttachmentService {

    AttachmentVO upload(Long defectId, MultipartFile file);

    List<AttachmentVO> listByDefect(Long defectId);

    ResponseEntity<Resource> download(Long id);

    void delete(Long id);
}
