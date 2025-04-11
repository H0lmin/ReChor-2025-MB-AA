package ch.epfl.rechor;

import java.util.List;
import java.util.Map;

public sealed interface Json permits Json.JArray, Json.JObject, Json.JString, Json.JNumber {

    // Represents a JSON array
    record JArray(List<Json> elements) implements Json {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < elements.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(elements.get(i).toString());
            }
            sb.append(']');
            return sb.toString();
        }
    }

    // Represents a JSON object
    record JObject(Map<String, Json> members) implements Json {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Json> entry : members.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(entry.getKey()).append('"').append(':')
                        .append(entry.getValue().toString());
            }
            sb.append('}');
            return sb.toString();
        }
    }

    // Represents a JSON string
    record JString(String s) implements Json {
        @Override
        public String toString() {
            return "\"" + s + "\"";
        }
    }

    // Represents a JSON number
    record JNumber(double value) implements Json {
        @Override
        public String toString() {
            return Double.toString(value);
        }
    }
}
