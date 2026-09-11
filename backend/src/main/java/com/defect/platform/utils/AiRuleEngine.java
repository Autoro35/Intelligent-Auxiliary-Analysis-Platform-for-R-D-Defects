package com.defect.platform.utils;

import cn.hutool.core.util.StrUtil;
import com.defect.platform.common.constant.DefectPriorityEnum;
import com.defect.platform.common.constant.DefectSeverityEnum;
import com.defect.platform.common.constant.DefectTypeEnum;
import com.defect.platform.vo.AiClassifyVO;
import com.defect.platform.vo.AiCompleteVO;
import com.defect.platform.vo.AiSuggestionVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 本地规则引擎（AI 能力的降级实现）
 * <p>当未配置 DeepSeek API Key 或大模型调用失败时，由本引擎基于关键词字典给出判定，
 * 保证平台在无外部依赖的情况下依然可演示、可验收。</p>
 * <p>本类为纯函数实现，不依赖数据库与网络。</p>
 */
@Component
public class AiRuleEngine {

    /** 类型关键词字典，key 为 DefectTypeEnum.code，按匹配权重（关键词长度和）取胜者 */
    private static final Map<String, List<String>> TYPE_KEYWORDS = new LinkedHashMap<>();

    /** 严重程度关键词 */
    private static final List<String> BLOCKER_KEYWORDS = Arrays.asList(
            "崩溃", "闪退", "宕机", "白屏", "死机", "不可用", "无法使用", "不能使用", "数据丢失",
            "数据错乱", "死锁", "内存泄漏", "服务不可用", "全部失败", "挂掉", "阻塞上线");
    private static final List<String> CRITICAL_KEYWORDS = Arrays.asList(
            "严重", "必现", "核心功能", "主要功能", "无法", "不能", "报错", "异常", "失败", "错误",
            "500", "404", "超时", "中断", "丢失", "阻塞");
    private static final List<String> MINOR_KEYWORDS = Arrays.asList(
            "轻微", "偶尔", "偶现", "个别", "文案", "样式", "排版", "提示", "建议", "优化", "体验",
            "美化", "措辞", "对齐", "颜色", "间距");

    /** 优先级提升关键词（命中后优先级上调一档） */
    private static final List<String> URGENT_KEYWORDS = Arrays.asList(
            "紧急", "线上", "生产", "客户投诉", "投诉", "发布", "发版", "阻塞", "影响范围大", "大面积");

    /** 2 字功能词，避免作为检索关键词产生噪声 */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "一个", "这个", "那个", "我们", "你们", "他们", "问题", "出现", "时候", "然后",
            "但是", "因为", "所以", "并且", "可以", "进行", "使用", "以及", "还是", "就是", "不是",
            "没有", "已经", "需要", "通过", "关于", "对于", "由于", "如果", "那么", "什么", "怎么",
            "有些", "一些", "部分", "情况", "时间", "地方", "一下", "之后", "之前", "目前", "现在",
            "开始", "应该", "可能", "一直", "总是", "经常", "非常", "特别", "而且", "或者"));

    static {
        TYPE_KEYWORDS.put(DefectTypeEnum.PERFORMANCE.getCode(), Arrays.asList(
                "性能", "慢", "卡顿", "卡死", "超时", "响应时间", "响应慢", "加载慢", "内存", "cpu",
                "占用", "并发", "吞吐", "延迟", "抖动", "高负载", "压力", "oom", "溢出", "耗时"));
        TYPE_KEYWORDS.put(DefectTypeEnum.UI.getCode(), Arrays.asList(
                "样式", "界面", "排版", "错位", "重叠", "遮挡", "颜色", "按钮", "图标", "布局",
                "对齐", "字体", "显示", "弹窗", "空白", "边框", "文案", "缩放", "遮挡", "页面样式"));
        TYPE_KEYWORDS.put(DefectTypeEnum.COMPATIBILITY.getCode(), Arrays.asList(
                "浏览器", "兼容", "chrome", "firefox", "safari", "edge", "安卓", "android", "ios",
                "苹果", "微信", "小程序", "机型", "分辨率", "操作系统", "版本差异", "国产化", "统信", "麒麟"));
        TYPE_KEYWORDS.put(DefectTypeEnum.OPTIMIZATION.getCode(), Arrays.asList(
                "建议", "优化", "改进", "提升", "体验", "希望", "考虑", "增强", "冗余", "简洁",
                "重构", "易用", "可读性", "方便", "建议增加", "建议优化"));
    }

    /**
     * 基于关键词规则判定缺陷类型、优先级与严重程度
     *
     * @param title       缺陷标题
     * @param description 缺陷描述，可为空
     */
    public AiClassifyVO classify(String title, String description) {
        String text = (StrUtil.blankToDefault(title, "") + " " + StrUtil.blankToDefault(description, ""))
                .toLowerCase();

        // 1. 类型：按命中关键词的字符总长度打分，取最高分（同分取字典声明顺序靠前者）
        String bestType = DefectTypeEnum.FUNCTIONAL.getCode();
        int bestScore = 0;
        List<String> bestHits = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : TYPE_KEYWORDS.entrySet()) {
            List<String> hits = matchAll(text, entry.getValue());
            int score = hits.stream().mapToInt(String::length).sum();
            if (score > bestScore) {
                bestScore = score;
                bestType = entry.getKey();
                bestHits = hits;
            }
        }

        // 2. 严重程度：致命 > 严重 > 轻微 > 一般（按优先级顺序短路判断）
        String severity;
        List<String> severityHits;
        if (!(severityHits = matchAll(text, BLOCKER_KEYWORDS)).isEmpty()) {
            severity = DefectSeverityEnum.BLOCKER.getCode();
        } else if (!(severityHits = matchAll(text, CRITICAL_KEYWORDS)).isEmpty()) {
            severity = DefectSeverityEnum.CRITICAL.getCode();
        } else if (!(severityHits = matchAll(text, MINOR_KEYWORDS)).isEmpty()) {
            severity = DefectSeverityEnum.MINOR.getCode();
        } else {
            severity = DefectSeverityEnum.MAJOR.getCode();
            severityHits = Collections.emptyList();
        }

        // 3. 优先级：先由严重程度映射，再按紧急关键词上调，最后受类型约束
        String priority = mapPriority(severity);
        List<String> urgentHits = matchAll(text, URGENT_KEYWORDS);
        if (!urgentHits.isEmpty()) {
            priority = promote(priority);
        }
        if (DefectTypeEnum.OPTIMIZATION.getCode().equals(bestType)) {
            // 建议优化类缺陷不应占用紧急/高优先级
            priority = DefectPriorityEnum.LOW.getCode();
        }

        AiClassifyVO vo = new AiClassifyVO();
        vo.setType(bestType);
        vo.setTypeDesc(DefectTypeEnum.descOf(bestType));
        vo.setPriority(priority);
        vo.setPriorityDesc(DefectPriorityEnum.descOf(priority));
        vo.setSeverity(severity);
        vo.setSeverityDesc(DefectSeverityEnum.descOf(severity));
        vo.setConfidence(confidence(bestHits.size(), severityHits.size()));
        vo.setReason(buildReason(bestType, bestHits, severityHits, urgentHits));
        vo.setSource("RULE");
        return vo;
    }

    /**
     * 抽取用于模糊检索的关键词：中文长串按 2 字切分（与 MySQL ngram_token_size 对齐），英文数字保留整词
     *
     * @param max 最多返回多少个关键词
     */
    public List<String> extractKeywords(String text, int max) {
        if (StrUtil.isBlank(text) || max <= 0) {
            return Collections.emptyList();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String segment : text.split("[^\\u4e00-\\u9fa5a-zA-Z0-9]+")) {
            if (result.size() >= max) {
                break;
            }
            if (StrUtil.isBlank(segment)) {
                continue;
            }
            if (isCjk(segment)) {
                if (segment.length() <= 4) {
                    addKeyword(result, segment);
                    continue;
                }
                for (int i = 0; i + 2 <= segment.length() && result.size() < max; i += 2) {
                    addKeyword(result, segment.substring(i, i + 2));
                }
            } else {
                addKeyword(result, segment);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 基于模板补全缺陷描述（无大模型时的降级输出）
     */
    public AiCompleteVO completeDescription(String title, String description, String module, String environment) {
        String moduleText = StrUtil.blankToDefault(module, "【待确认模块】");
        String titleText = StrUtil.blankToDefault(title, "该问题");

        String steps = "1. 登录系统，进入「" + moduleText + "」功能；\n"
                + "2. 执行与「" + titleText + "」相关的操作；\n"
                + "3. 观察页面展示与接口返回，复现异常现象。";
        String expected = "操作应正常完成，「" + titleText + "」所述现象不应出现，数据与页面展示符合预期。";
        String actual = StrUtil.blankToDefault(description, "实际操作后出现「" + titleText + "」所述异常现象。");
        String env = StrUtil.blankToDefault(environment, "浏览器/客户端：Chrome 最新版；操作系统：Windows 11；被测版本：当前迭代版本。");
        String phenomenon = titleText + "。" + actual;

        List<AiSuggestionVO> suggestions = new ArrayList<>();
        suggestions.add(new AiSuggestionVO("reproduceSteps", "复现步骤", steps));
        suggestions.add(new AiSuggestionVO("expectedResult", "预期结果", expected));
        suggestions.add(new AiSuggestionVO("actualResult", "实际结果", actual));
        suggestions.add(new AiSuggestionVO("environment", "运行环境", env));

        List<String> questions = new ArrayList<>();
        if (StrUtil.isBlank(description)) {
            questions.add("请补充实际现象的具体表现（报错信息、控制台日志或截图）。");
        }
        if (StrUtil.isBlank(module)) {
            questions.add("请确认该缺陷所属的功能模块，便于指派处理人。");
        }
        if (StrUtil.isBlank(environment)) {
            questions.add("请补充运行环境（浏览器/操作系统/客户端版本）。");
        }
        questions.add("该问题是否稳定复现？复现频率大概是多少？");
        questions.add("是否影响线上环境？当前是否有临时规避方案？");

        AiCompleteVO vo = new AiCompleteVO();
        vo.setCompletedDescription(buildDescription(phenomenon, steps, expected, actual, env));
        vo.setSuggestions(suggestions);
        vo.setQuestions(questions);
        vo.setSource("RULE");
        return vo;
    }

    /**
     * 组装规范化的缺陷描述正文
     */
    public String buildDescription(String phenomenon, String steps,
                                   String expected, String actual, String environment) {
        return "【问题现象】\n" + phenomenon + "\n\n"
                + "【复现步骤】\n" + steps + "\n\n"
                + "【预期结果】\n" + expected + "\n\n"
                + "【实际结果】\n" + actual + "\n\n"
                + "【运行环境】\n" + environment;
    }

    // ---- 私有 ----

    /** 返回文本中命中的全部关键词（保序） */
    private List<String> matchAll(String text, List<String> keywords) {
        List<String> hits = new ArrayList<>();
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                hits.add(keyword);
            }
        }
        return hits;
    }

    private String mapPriority(String severity) {
        if (DefectSeverityEnum.BLOCKER.getCode().equals(severity)) {
            return DefectPriorityEnum.URGENT.getCode();
        }
        if (DefectSeverityEnum.CRITICAL.getCode().equals(severity)) {
            return DefectPriorityEnum.HIGH.getCode();
        }
        if (DefectSeverityEnum.MINOR.getCode().equals(severity)) {
            return DefectPriorityEnum.LOW.getCode();
        }
        return DefectPriorityEnum.MEDIUM.getCode();
    }

    /** 优先级上调一档，已是最高则保持不变 */
    private String promote(String priority) {
        DefectPriorityEnum[] values = DefectPriorityEnum.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getCode().equals(priority) && i > 0) {
                return values[i - 1].getCode();
            }
        }
        return priority;
    }

    private double confidence(int typeHits, int severityHits) {
        int hits = typeHits + severityHits;
        if (hits >= 3) {
            return 0.85;
        }
        if (hits == 2) {
            return 0.7;
        }
        if (hits == 1) {
            return 0.55;
        }
        return 0.35;
    }

    private String buildReason(String type, List<String> typeHits, List<String> severityHits, List<String> urgentHits) {
        StringBuilder sb = new StringBuilder();
        if (typeHits.isEmpty()) {
            sb.append("未命中类型关键词，默认归类为").append(DefectTypeEnum.descOf(type));
        } else {
            sb.append("命中").append(DefectTypeEnum.descOf(type)).append("关键词：")
                    .append(String.join("、", typeHits));
        }
        if (!severityHits.isEmpty()) {
            sb.append("；严重程度命中：").append(String.join("、", severityHits));
        }
        if (!urgentHits.isEmpty()) {
            sb.append("；紧急关键词命中：").append(String.join("、", urgentHits));
        }
        return sb.append("（本地规则引擎判定）").toString();
    }

    /**
     * 收录关键词：中文单字也保留（MySQL ngram 最小切分长度为 2，单字只能靠模糊匹配命中）
     */
    private void addKeyword(Set<String> result, String keyword) {
        if (StrUtil.isBlank(keyword) || STOP_WORDS.contains(keyword)
                || keyword.chars().allMatch(Character::isDigit)) {
            return;
        }
        int minLength = isCjk(keyword) ? 1 : 2;
        if (keyword.length() < minLength) {
            return;
        }
        result.add(keyword);
    }

    private boolean isCjk(String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        return text.codePointAt(0) >= 0x4E00 && text.codePointAt(0) <= 0x9FA5;
    }
}
