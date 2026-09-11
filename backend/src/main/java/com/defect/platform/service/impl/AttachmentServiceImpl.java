package com.defect.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.defect.platform.common.ResultCode;
import com.defect.platform.common.context.UserContext;
import com.defect.platform.common.exception.BusinessException;
import com.defect.platform.entity.Attachment;
import com.defect.platform.entity.Defect;
import com.defect.platform.entity.User;
import com.defect.platform.mapper.AttachmentMapper;
import com.defect.platform.mapper.DefectMapper;
import com.defect.platform.mapper.UserMapper;
import com.defect.platform.service.AttachmentService;
import com.defect.platform.service.ProjectService;
import com.defect.platform.vo.AttachmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 附件服务实现：本地磁盘存储，路径按日期分目录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private static final String BUSINESS_TYPE_DEFECT = "DEFECT";

    private final AttachmentMapper attachmentMapper;
    private final DefectMapper defectMapper;
    private final ProjectService projectService;
    private final UserMapper userMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public AttachmentVO upload(Long defectId, MultipartFile file) {
        Defect defect = defectMapper.selectById(defectId);
        if (defect == null) {
            throw new BusinessException(ResultCode.DEFECT_NOT_FOUND);
        }
        projectService.assertMember(defect.getProjectId());
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "上传文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String ext = StrUtil.isBlank(originalName) ? "" : StrUtil.subAfter(originalName, ".", true);
        String storedName = UUID.randomUUID().toString().replace("-", "")
                + (StrUtil.isBlank(ext) ? "" : "." + ext);
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        Path dir = Paths.get(uploadDir, dateDir);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(storedName));
        } catch (IOException e) {
            log.error("文件存储失败", e);
            throw new BusinessException(ResultCode.ERROR.getCode(), "文件存储失败");
        }

        Attachment attachment = new Attachment();
        attachment.setBusinessType(BUSINESS_TYPE_DEFECT);
        attachment.setBusinessId(defectId);
        attachment.setFileName(originalName);
        attachment.setFilePath(dateDir + "/" + storedName);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setUploaderId(UserContext.getUserId());
        attachmentMapper.insert(attachment);

        return toVO(attachment);
    }

    @Override
    public List<AttachmentVO> listByDefect(Long defectId) {
        Defect defect = defectMapper.selectById(defectId);
        if (defect == null) {
            throw new BusinessException(ResultCode.DEFECT_NOT_FOUND);
        }
        projectService.assertMember(defect.getProjectId());
        List<Attachment> list = attachmentMapper.selectList(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getBusinessType, BUSINESS_TYPE_DEFECT)
                .eq(Attachment::getBusinessId, defectId)
                .orderByDesc(Attachment::getId));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<Resource> download(Long id) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "附件不存在");
        }
        Resource resource = new FileSystemResource(Paths.get(uploadDir, attachment.getFilePath()));
        if (!resource.exists()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "附件文件不存在");
        }
        ContentDisposition cd = ContentDisposition.attachment()
                .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        StrUtil.blankToDefault(attachment.getContentType(), "application/octet-stream")))
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .body(resource);
    }

    @Override
    public void delete(Long id) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "附件不存在");
        }
        // 校验项目访问权限
        Defect defect = defectMapper.selectById(attachment.getBusinessId());
        if (defect != null) {
            projectService.assertMember(defect.getProjectId());
        }
        attachmentMapper.deleteById(id);
        // 删除物理文件（失败仅告警，不影响逻辑删除）
        try {
            Files.deleteIfExists(Paths.get(uploadDir, attachment.getFilePath()));
        } catch (IOException e) {
            log.warn("删除附件物理文件失败: {}", attachment.getFilePath());
        }
    }

    private AttachmentVO toVO(Attachment a) {
        AttachmentVO vo = new AttachmentVO();
        vo.setId(a.getId());
        vo.setFileName(a.getFileName());
        vo.setFileSize(a.getFileSize());
        vo.setContentType(a.getContentType());
        vo.setUploaderId(a.getUploaderId());
        vo.setCreateTime(a.getCreateTime());
        User u = userMapper.selectById(a.getUploaderId());
        vo.setUploaderName(u == null ? null : StrUtil.blankToDefault(u.getNickname(), u.getUsername()));
        return vo;
    }
}
