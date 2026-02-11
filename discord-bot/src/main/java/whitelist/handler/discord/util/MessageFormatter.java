package whitelist.handler.discord.util;

import java.util.Map;

public class MessageFormatter {
    public static String format(String template, Map<String, String> values) {
        if (template == null) return "";
        String out = template;
        if (values != null) {
            for (Map.Entry<String, String> e : values.entrySet()) {
                out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
            }
        }
        return out;
    }
}
