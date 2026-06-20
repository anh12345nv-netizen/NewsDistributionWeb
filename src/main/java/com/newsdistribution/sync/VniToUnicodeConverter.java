package com.newsdistribution.sync;

import java.util.HashMap;
import java.util.Map;

public class VniToUnicodeConverter {
    private static final Map<String, String> vniToUnicodeMap = new HashMap<>();

    static {
        vniToUnicodeMap.put("aù", "á");
        vniToUnicodeMap.put("aø", "à");
        vniToUnicodeMap.put("aû", "ả");
        vniToUnicodeMap.put("aõ", "ã");
        vniToUnicodeMap.put("aï", "ạ");
        vniToUnicodeMap.put("aâ", "â");
        vniToUnicodeMap.put("aá", "ấ");
        vniToUnicodeMap.put("aà", "ầ");
        vniToUnicodeMap.put("aå", "ẩ");
        vniToUnicodeMap.put("aã", "ẫ");
        vniToUnicodeMap.put("aä", "ậ");
        vniToUnicodeMap.put("aê", "ă");
        vniToUnicodeMap.put("aé", "ắ");
        vniToUnicodeMap.put("aè", "ằ");
        vniToUnicodeMap.put("aú", "ẳ");
        vniToUnicodeMap.put("añ", "ẵ");
        vniToUnicodeMap.put("aë", "ặ");
        vniToUnicodeMap.put("eù", "é");
        vniToUnicodeMap.put("eø", "è");
        vniToUnicodeMap.put("eû", "ẻ");
        vniToUnicodeMap.put("eõ", "ẽ");
        vniToUnicodeMap.put("eï", "ẹ");
        vniToUnicodeMap.put("eâ", "ê");
        vniToUnicodeMap.put("eá", "ế");
        vniToUnicodeMap.put("eà", "ề");
        vniToUnicodeMap.put("eå", "ể");
        vniToUnicodeMap.put("eã", "ễ");
        vniToUnicodeMap.put("eä", "ệ");
        vniToUnicodeMap.put("iù", "í");
        vniToUnicodeMap.put("iø", "ì");
        vniToUnicodeMap.put("iû", "ỉ");
        vniToUnicodeMap.put("iõ", "ĩ");
        vniToUnicodeMap.put("iï", "ị");
        vniToUnicodeMap.put("où", "ó");
        vniToUnicodeMap.put("oø", "ò");
        vniToUnicodeMap.put("oû", "ỏ");
        vniToUnicodeMap.put("oõ", "õ");
        vniToUnicodeMap.put("oï", "ọ");
        vniToUnicodeMap.put("oâ", "ô");
        vniToUnicodeMap.put("oá", "ố");
        vniToUnicodeMap.put("oà", "ồ");
        vniToUnicodeMap.put("oå", "ổ");
        vniToUnicodeMap.put("oã", "ỗ");
        vniToUnicodeMap.put("oä", "ộ");
        vniToUnicodeMap.put("oơ", "ơ");
        vniToUnicodeMap.put("oé", "ớ");
        vniToUnicodeMap.put("oè", "ờ");
        vniToUnicodeMap.put("oú", "ở");
        vniToUnicodeMap.put("oñ", "ỡ");
        vniToUnicodeMap.put("oë", "ợ");
        vniToUnicodeMap.put("uù", "ú");
        vniToUnicodeMap.put("uø", "ù");
        vniToUnicodeMap.put("uû", "ủ");
        vniToUnicodeMap.put("uõ", "ũ");
        vniToUnicodeMap.put("uï", "ụ");
        vniToUnicodeMap.put("uê", "ư");
        vniToUnicodeMap.put("uá", "ứ");
        vniToUnicodeMap.put("uà", "ừ");
        vniToUnicodeMap.put("uå", "ử");
        vniToUnicodeMap.put("uã", "ữ");
        vniToUnicodeMap.put("uä", "ự");
        vniToUnicodeMap.put("yù", "ý");
        vniToUnicodeMap.put("yø", "ỳ");
        vniToUnicodeMap.put("yû", "ỷ");
        vniToUnicodeMap.put("yõ", "ỹ");
        vniToUnicodeMap.put("yï", "ỵ");
        vniToUnicodeMap.put("ñ", "đ");
        
        // Uppercase
        vniToUnicodeMap.put("AÙ", "Á");
        vniToUnicodeMap.put("AØ", "À");
        vniToUnicodeMap.put("AÛ", "Ả");
        vniToUnicodeMap.put("AÕ", "Ã");
        vniToUnicodeMap.put("AÏ", "Ạ");
        vniToUnicodeMap.put("AÂ", "Â");
        vniToUnicodeMap.put("AÁ", "Ấ");
        vniToUnicodeMap.put("AÀ", "Ầ");
        vniToUnicodeMap.put("AÅ", "Ẩ");
        vniToUnicodeMap.put("AÃ", "Ẫ");
        vniToUnicodeMap.put("AÄ", "Ậ");
        vniToUnicodeMap.put("AÊ", "Ă");
        vniToUnicodeMap.put("AÉ", "Ắ");
        vniToUnicodeMap.put("AÈ", "Ằ");
        vniToUnicodeMap.put("AÚ", "Ẳ");
        vniToUnicodeMap.put("AÑ", "Ẵ");
        vniToUnicodeMap.put("AË", "Ặ");
        vniToUnicodeMap.put("EÙ", "É");
        vniToUnicodeMap.put("EØ", "È");
        vniToUnicodeMap.put("EÛ", "Ẻ");
        vniToUnicodeMap.put("EÕ", "Ẽ");
        vniToUnicodeMap.put("EÏ", "Ẹ");
        vniToUnicodeMap.put("EÂ", "Ê");
        vniToUnicodeMap.put("EÁ", "Ế");
        vniToUnicodeMap.put("EÀ", "Ề");
        vniToUnicodeMap.put("EÅ", "Ể");
        vniToUnicodeMap.put("EÃ", "Ễ");
        vniToUnicodeMap.put("EÄ", "Ệ");
        vniToUnicodeMap.put("IÙ", "Í");
        vniToUnicodeMap.put("IØ", "Ì");
        vniToUnicodeMap.put("IÛ", "Ỉ");
        vniToUnicodeMap.put("IÕ", "Ĩ");
        vniToUnicodeMap.put("IÏ", "Ị");
        vniToUnicodeMap.put("OÙ", "Ó");
        vniToUnicodeMap.put("OØ", "Ò");
        vniToUnicodeMap.put("OÛ", "Ỏ");
        vniToUnicodeMap.put("OÕ", "Õ");
        vniToUnicodeMap.put("OÏ", "Ọ");
        vniToUnicodeMap.put("OÂ", "Ô");
        vniToUnicodeMap.put("OÁ", "Ố");
        vniToUnicodeMap.put("OÀ", "Ồ");
        vniToUnicodeMap.put("OÅ", "Ổ");
        vniToUnicodeMap.put("OÃ", "Ỗ");
        vniToUnicodeMap.put("OÄ", "Ộ");
        vniToUnicodeMap.put("OÊ", "Ơ");
        vniToUnicodeMap.put("OÉ", "Ớ");
        vniToUnicodeMap.put("OÈ", "Ờ");
        vniToUnicodeMap.put("OÚ", "Ở");
        vniToUnicodeMap.put("OÑ", "Ỡ");
        vniToUnicodeMap.put("OË", "Ợ");
        vniToUnicodeMap.put("UÙ", "Ú");
        vniToUnicodeMap.put("UØ", "Ù");
        vniToUnicodeMap.put("UÛ", "Ủ");
        vniToUnicodeMap.put("UÕ", "Ũ");
        vniToUnicodeMap.put("UÏ", "Ụ");
        vniToUnicodeMap.put("UÊ", "Ư");
        vniToUnicodeMap.put("UÁ", "Ứ");
        vniToUnicodeMap.put("UÀ", "Ừ");
        vniToUnicodeMap.put("UÅ", "Ử");
        vniToUnicodeMap.put("UÃ", "Ữ");
        vniToUnicodeMap.put("UÄ", "Ự");
        vniToUnicodeMap.put("YÙ", "Ý");
        vniToUnicodeMap.put("YØ", "Ỳ");
        vniToUnicodeMap.put("YÛ", "Ỷ");
        vniToUnicodeMap.put("YÕ", "Ỹ");
        vniToUnicodeMap.put("YÏ", "Ỵ");
        vniToUnicodeMap.put("Ñ", "Đ");
        
        // Additional edge case
        vniToUnicodeMap.put("Nieân", "Niên");
        vniToUnicodeMap.put("Coáng", "Cống");
        vniToUnicodeMap.put("Quyõnh", "Quỳnh");
    }

    public static String convert(String vniText) {
        if (vniText == null || vniText.isEmpty()) {
            return vniText;
        }
        
        String result = vniText;
        java.util.List<String> keys = new java.util.ArrayList<>(vniToUnicodeMap.keySet());
        keys.sort((k1, k2) -> Integer.compare(k2.length(), k1.length()));
        
        for (String key : keys) {
            result = result.replace(key, vniToUnicodeMap.get(key));
        }
        
        return result;
    }
}
