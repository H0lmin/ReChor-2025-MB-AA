package ch.epfl.rechor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a JSON value: array, object, string, or number.
 */
public sealed interface Json {

    /**
     * JSON array value.
     *
     * @param elements the elements of the JSON array
     */
    record JArray(List<Json> elements) implements Json {
        @Override
        public String toString() {
            String content = elements.stream()
                    .map(Json::toString)
                    .collect(Collectors.joining(","));
            return "[" + content + "]";
        }
    }

    /**
     * JSON object value.
     *
     * @param members mapping of JSON object keys to values
     */
    record JObject(Map<String, Json> members) implements Json {
        @Override
        public String toString() {
            String content = members.entrySet().stream()
                    .map(entry -> '"' + entry.getKey() + "\":" + entry.getValue())
                    .collect(Collectors.joining(","));
            return "{" + content + "}";
        }
    }

    /**
     * JSON string value.
     *
     * @param text the string content (unescaped)
     */
    record JString(String text) implements Json {
        @Override
        public String toString() {
            return "\"" + text + "\"";
        }
    }

    /**
     * JSON number value.
     *
     * @param value the numeric value
     */
    record JNumber(double value) implements Json {
        @Override
        public String toString() {
            return Double.toString(value);
        }
    }
}