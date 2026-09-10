package com.defect.platform.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缺陷操作类型枚举（用于操作日志留痕）
 */
@Getter
@AllArgsConstructor
public enum DefectActionEnum {

    CREATE("CREATE", "创建缺陷"),
    UPDATE("UPDATE", "编辑缺陷"),
    ASSIGN("ASSIGN", "分配缺陷"),
    START("START", "开始处理"),
    RESOLVE("RESOLVE", "标记解决"),
    RETEST("RETEST", "复测"),
    CLOSE("CLOSE", "关闭缺陷"),
    REJECT("REJECT", "驳回缺陷"),
    REOPEN("REOPEN", "重新打开"),
    COMMENT("COMMENT", "添加评论");

    private final String code;
    private final String desc;

    /**
     * 根据 code 获取中文描述，未匹配时返回原值
     */
    public static String descOf(String code) {
        for (DefectActionEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return code;
    }
}
