package com.defect.platform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.defect.platform.common.PageResult;
import com.defect.platform.common.ResultCode;
import com.defect.platform.common.constant.CacheKeys;
import com.defect.platform.common.context.UserContext;
import com.defect.platform.common.exception.BusinessException;
import com.defect.platform.dto.KnowledgeDTO;
import com.defect.platform.entity.Defect;
import com.defect.platform.entity.Knowledge;
import com.defect.platform.entity.User;
import com.defect.platform.mapper.DefectMapper;
import com.defect.platform.mapper.KnowledgeMapper;
import com.defect.platform.mapper.UserMapper;
import com.defect.platform.service.CacheService;
import com.defect.platform.service.KnowledgeRetrievalService;
import com.defect.platform.service.KnowledgeService;
import com.defect.platform.vo.KnowledgeVO;
import com.defect.platform.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 * <p>知识库为团队全局共享（不限项目）；浏览计数原子自增；从缺陷沉淀时自动带入类型/根因/方案</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, Knowledge> implements KnowledgeService {

    /** 标签云缓存有效期：写操作会主动失效，这里只是兜底 TTL */
    private static final Duration TAGS_TTL = Duration.ofMinutes(10);

    private final DefectMapper defectMapper;
    private final UserMapper userMapper;
    /** 向量索引维护（尽力而为，向量库不可用时静默忽略，不影响知识库主流程） */
    private final KnowledgeRetrievalService retrievalService;
    private final CacheService cacheService;

    @Override
    public KnowledgeVO create(KnowledgeDTO dto) {
        Knowledge knowledge = new Knowledge();
        BeanUtil.copyProperties(dto, knowledge);
        knowledge.setViewCount(0);
        knowledge.setCreateBy(UserContext.getUserId());
        save(knowledge);
        retrievalService.index(knowledge);
        evictCache();
        log.info("新增知识: id={}, title={}", knowledge.getId(), knowledge.getTitle());
        return toVO(knowledge);
    }

    @Override
    public KnowledgeVO update(Long id, KnowledgeDTO dto) {
        Knowledge knowledge = getById(id);
        if (knowledge == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
        }
        // 来源缺陷不允许改绑，仅编辑描述性字段
        BeanUtil.copyProperties(dto, knowledge, "defectId");
        knowledge.setId(id);
        updateById(knowledge);
        Knowledge updated = getById(id);
        // 正文变更后同步刷新向量，保证 RAG 检索到的是最新内容
        retrievalService.index(updated);
        evictCache();
        return toVO(updated);
    }

    @Override
    public void delete(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
        }
        removeById(id);
        retrievalService.remove(id);
        evictCache();
    }

    @Override
    public KnowledgeVO get(Long id) {
        Knowledge knowledge = getById(id);
        if (knowledge == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_NOT_FOUND);
        }
        // 浏览计数原子自增（避免并发覆盖）
        update(new LambdaUpdateWrapper<Knowledge>()
                .eq(Knowledge::getId, id)
                .setSql("view_count = view_count + 1"));
        int view = knowledge.getViewCount() == null ? 0 : knowledge.getViewCount();
        knowledge.setViewCount(view + 1);
        return toVO(knowledge);
    }

    @Override
    public PageResult<KnowledgeVO> list(long current, long size, String keyword, String type, String tag) {
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        // 关键字在标题/根因/方案中模糊匹配（嵌套 OR）
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Knowledge::getTitle, keyword)
                    .or().like(Knowledge::getRootCause, keyword)
                    .or().like(Knowledge::getSolution, keyword));
        }
        wrapper.eq(StrUtil.isNotBlank(type), Knowledge::getType, type)
                .like(StrUtil.isNotBlank(tag), Knowledge::getTags, tag)
                .orderByDesc(Knowledge::getId);
        Page<Knowledge> knowledgePage = page(new Page<>(current, size), wrapper);
        // 一次性批量加载本页创建人，避免逐行查询（N+1）
        Map<Long, User> userMap = batchUsers(knowledgePage.getRecords().stream()
                .map(Knowledge::getCreateBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return PageResult.of(knowledgePage, k -> toVO(k, userMap));
    }

    @Override
    public KnowledgeVO precipitate(Long defectId, KnowledgeDTO dto) {
        Defect defect = defectMapper.selectById(defectId);
        if (defect == null) {
            throw new BusinessException(ResultCode.DEFECT_NOT_FOUND);
        }
        // 沉淀前提：缺陷已填写解决方案
        if (StrUtil.isBlank(defect.getSolution())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "缺陷尚未填写解决方案，无法沉淀为知识");
        }
        // 同一缺陷只沉淀一次
        long exists = count(new LambdaQueryWrapper<Knowledge>().eq(Knowledge::getDefectId, defectId));
        if (exists > 0) {
            throw new BusinessException(ResultCode.KNOWLEDGE_EXISTS);
        }

        Knowledge knowledge = new Knowledge();
        knowledge.setTitle(StrUtil.blankToDefault(dto.getTitle(), defect.getTitle()));
        knowledge.setDefectId(defectId);
        knowledge.setType(defect.getType());
        knowledge.setRootCause(StrUtil.blankToDefault(dto.getRootCause(), defect.getRootCause()));
        knowledge.setSolution(defect.getSolution());
        knowledge.setTags(dto.getTags());
        knowledge.setViewCount(0);
        knowledge.setCreateBy(UserContext.getUserId());
        save(knowledge);
        retrievalService.index(knowledge);
        evictCache();
        log.info("沉淀知识: defectId={}, knowledgeId={}", defectId, knowledge.getId());
        return toVO(knowledge);
    }

    @Override
    public List<TagVO> tags() {
        // 标签云为全局共享数据，与用户无关，可直接按前缀缓存
        return cacheService.getList(CacheKeys.knowledgeTags(), TAGS_TTL, TagVO.class, this::loadTags);
    }

    private List<TagVO> loadTags() {
        List<Knowledge> list = list(new LambdaQueryWrapper<Knowledge>().isNotNull(Knowledge::getTags));
        Map<String, Long> counter = new HashMap<>();
        for (Knowledge k : list) {
            if (StrUtil.isBlank(k.getTags())) {
                continue;
            }
            Arrays.stream(k.getTags().split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .forEach(t -> counter.merge(t, 1L, Long::sum));
        }
        // 按出现次数降序，形成标签云
        return counter.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new TagVO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    // ---- 私有 ----

    /** 单条场景（创建/更新/详情/沉淀）：只为这一条记录加载创建人 */
    private KnowledgeVO toVO(Knowledge k) {
        return toVO(k, batchUsers(k.getCreateBy() == null
                ? Collections.emptySet()
                : Collections.singleton(k.getCreateBy())));
    }

    /** 批量场景：创建人表由调用方预先批量加载，此处不再查库 */
    private KnowledgeVO toVO(Knowledge k, Map<Long, User> userMap) {
        KnowledgeVO vo = KnowledgeVO.from(k);
        if (k.getCreateBy() != null) {
            User u = userMap.get(k.getCreateBy());
            vo.setCreateByName(u == null ? null : StrUtil.blankToDefault(u.getNickname(), u.getUsername()));
        }
        return vo;
    }

    /**
     * 知识库变动后清理相关缓存
     * <p>标签云受本次变更直接影响；统计总览里的「知识库条目数」也会变，因此统计缓存一并失效</p>
     */
    private void evictCache() {
        cacheService.evict(CacheKeys.knowledgeTags());
        cacheService.evictByPrefix(CacheKeys.STATS_PREFIX);
    }

    /** 按 ID 集合批量取用户，返回 id -> User 映射 */
    private Map<Long, User> batchUsers(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }
}
