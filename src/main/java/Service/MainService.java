package Service;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

class MainService {
    private final String SIGN = "!";

    protected Boolean checkSign(final String content) {
        return content.startsWith(SIGN);
    }

    protected String format(final String content) {
        return new StringBuilder(content).deleteCharAt(0).toString();
    }

    protected String getJsonValue(final String key) {
        final JSONParser jsonParser = new JSONParser();
        try {
            final JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader("src/main/resources/instruction.json"));
            if (Optional.ofNullable(jsonObject.get(key)).isPresent()) {
                return jsonObject.get(key).toString();
            }
        } catch (final IOException | ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected String getJsonValue(final String outerKey, final String innerKey) {
        final JSONParser jsonParser = new JSONParser();
        try {
            final JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader("src/main/resources/data.json"));
            if (Optional.ofNullable(jsonObject.get(outerKey)).isPresent() && jsonObject.get(outerKey) instanceof JSONObject) {
                final JSONObject innerJsonObject = (JSONObject) jsonObject.get(outerKey);
                if (Optional.ofNullable(innerJsonObject.get(innerKey)).isPresent()) {
                    return innerJsonObject.get(innerKey).toString();
                }
            }
        } catch (final IOException | ParseException e) {
            e.printStackTrace();
        }
        return "";
    }
}
