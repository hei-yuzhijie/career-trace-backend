package com.yuzhijie.career.trace.module.tag.support;

import net.sourceforge.pinyin4j.PinyinHelper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 标签归一化工具：
 * <ul>
 *     <li>{@link #toSlug(String)}：把中文/英文/数字转成与 tags.slug 一致的拼音风格 slug，如“内容策划” → nei_rong_ce_hua；</li>
 *     <li>{@link #normalizeText(String)}：生成忽略大小写/空格/标点的匹配键，用于 tag_aliases.alias 的精确文本匹配。</li>
 * </ul>
 * 只做完整词归一化，不做子串匹配（见需求文档“同义标签的子串歧义”）。
 */
public final class SlugUtil {

    private SlugUtil() {
    }

    public static String toSlug(String raw) {
        if (raw == null) {
            return "";
        }
        String text = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        StringBuilder ascii = new StringBuilder();

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);

            if (cp < 128 && (Character.isLetterOrDigit(cp))) {
                ascii.append((char) cp);
                continue;
            }
            flushAscii(ascii, tokens);

            if (isCjk(cp)) {
                String pinyin = pinyinOf(cp);
                if (!pinyin.isEmpty()) {
                    tokens.add(pinyin);
                } else {
                    tokens.add("u" + Integer.toHexString(cp));
                }
            } else if (cp == 'ü' || cp == 'Ü') {
                ascii.append('v');
            }
            // 其他字符（空格、标点、斜杠等）直接作为分隔符
        }
        flushAscii(ascii, tokens);

        String slug = String.join("_", tokens);
        if (slug.length() > 90) {
            slug = slug.substring(0, 90);
        }
        if (slug.isEmpty()) {
            slug = "tag_" + Integer.toHexString(raw.hashCode());
        }
        return slug;
    }

    /**
     * 只保留字母、数字与汉字，并转小写。用于 alias 文本的精确匹配键。
     */
    public static String normalizeText(String raw) {
        if (raw == null) {
            return "";
        }
        String text = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp < 128 && Character.isLetterOrDigit(cp)) {
                sb.append((char) cp);
            } else if (isCjk(cp)) {
                sb.appendCodePoint(cp);
            }
        }
        return sb.toString();
    }

    private static void flushAscii(StringBuilder ascii, List<String> tokens) {
        if (ascii.length() > 0) {
            tokens.add(ascii.toString());
            ascii.setLength(0);
        }
    }

    private static boolean isCjk(int cp) {
        return Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN;
    }

    private static String pinyinOf(int cp) {
        try {
            String[] pinyin = PinyinHelper.toHanyuPinyinStringArray((char) cp);
            if (pinyin == null || pinyin.length == 0) {
                return "";
            }
            String value = pinyin[0];
            StringBuilder sb = new StringBuilder(value.length());
            for (int i = 0; i < value.length(); i++) {
                char c = Character.toLowerCase(value.charAt(i));
                if (c >= 'a' && c <= 'z') {
                    sb.append(c);
                } else if (c == 'ü' || c == 'v') {
                    sb.append('v');
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
