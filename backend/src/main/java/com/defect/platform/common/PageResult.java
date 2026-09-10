package com.defect.platform.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应结果体，适配前端分页组件（total/current/size/records）
 *
 * @param <T> 记录类型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Long current;

    /** 每页条数 */
    private Long size;

    /** 数据列表 */
    private List<T> records;

    private PageResult() {
    }

    /**
     * 将 MyBatis-Plus 分页对象转换为统一分页结果体
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setRecords(page.getRecords());
        return result;
    }

    /**
     * 将 MyBatis-Plus 分页对象转换为统一分页结果体，并逐条映射记录类型（如实体转 VO）
     */
    public static <S, T> PageResult<T> of(IPage<S> page, Function<S, T> mapper) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setRecords(page.getRecords().stream().map(mapper).collect(Collectors.toList()));
        return result;
    }
}
