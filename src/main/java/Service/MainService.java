package Service;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

class MainService {
    private final String SIGN = "!";
    protected final String REPLYTEXT = "replyText";
    protected final String IMAGE = "Image";
    protected final String YOUTUBEVIDEO = "YoutubeVideo";
    protected final String YOUTUBECHANNEL = "YoutubeChannel";
    protected final String TWITCHCHANNEL = "TwitchChannel";

    protected Boolean checkSign(final String content) {
        return content.startsWith(SIGN);
    }

    protected String format(final String content) {
        return new StringBuilder(content).delete(0, SIGN.length()).toString();
    }

    protected String[] getJsonValue(final String key) {
        final JSONParser jsonParser = new JSONParser();
        try {
            final JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader("src/main/resources/instruction.json"));
            if (jsonObject.containsKey(key) && jsonObject.get(key) instanceof JSONArray) {
                final JSONArray jsonArray = (JSONArray) jsonObject.get(key);
                return (String[]) jsonArray.toArray(new String[jsonArray.size()]);
            }
        } catch (final IOException | ParseException e) {
            e.printStackTrace();
        }
        return new String[0];
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
