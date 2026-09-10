package com.defect.platform.controller;

import com.defect.platform.common.Result;
import com.defect.platform.service.AttachmentService;
import com.defect.platform.vo.AttachmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 附件接口：上传、下载、删除
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/defects/{defectId}/attachments")
    public Result<AttachmentVO> upload(@PathVariable Long defectId, @RequestParam("file") MultipartFile file) {
        return Result.success("上传成功", attachmentService.upload(defectId, file));
    }

    @GetMapping("/defects/{defectId}/attachments")
    public Result<List<AttachmentVO>> list(@PathVariable Long defectId) {
        return Result.success(attachmentService.listByDefect(defectId));
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return attachmentService.download(id);
    }

    @DeleteMapping("/attachments/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return Result.success();
    }
}
