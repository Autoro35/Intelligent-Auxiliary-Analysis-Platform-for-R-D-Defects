package com.defect.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.defect.platform.entity.Knowledge;
import com.defect.platform.vo.KnowledgeSearchHitVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库 Mapper
 */
public interface KnowledgeMapper extends BaseMapper<Knowledge> {

    /**
     * 基于 ngram 全文索引检索知识（对应 t_knowledge 的 ft_knowledge 索引）
     * <p>中文无需分词，由 MySQL ngram 解析器按 ngram_token_size 切分，适合缺陷标题这类短文本检索</p>
     *
     * @param keyword 检索文本
     * @param limit   返回条数上限
     * @return 按相关度倒序的命中项
     */
    @Select("SELECT id, title, type, root_cause, solution, tags, "
            + "MATCH(title, root_cause, solution) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) AS score "
            + "FROM t_knowledge "
            + "WHERE deleted = 0 "
            + "AND MATCH(title, root_cause, solution) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) "
            + "ORDER BY score DESC LIMIT #{limit}")
    List<KnowledgeSearchHitVO> searchFulltext(@Param("keyword") String keyword, @Param("limit") int limit);
}
