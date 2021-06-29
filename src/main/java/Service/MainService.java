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
    protected final String IMAGE = "Image";
    protected final String YOUTUBEVIDEO = "YoutubeVideo";
    protected final String YOUTUBECHANNEL = "YoutubeChannel";
    protected final String TWITCHCHANNEL = "TwitchChannel";

    protected Boolean checkSign(final String content) {
        return content.startsWith(SIGN);
    }

    protected String format(final String content) {
        final String instruction = content.split(" ")[0];
        return new StringBuilder(instruction).delete(0, SIGN.length()).toString();
    }

    protected Optional<String[]> getAction(final String key) {
        final JSONParser jsonParser = new JSONParser();
        try {
            final JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader("src/main/resources/action.json"));
            final Optional<Object> jsonOptionalObject = Optional.ofNullable(jsonObject.get(key));
            if (jsonOptionalObject.isPresent()) {
                final JSONArray jsonArray = (JSONArray) jsonOptionalObject.get();
                return Optional.ofNullable((String[]) jsonArray.toArray(new String[jsonArray.size()]));
            }
        } catch (final IOException | ParseException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    protected Optional<String> getURL(final String outerKey, final String innerKey) {
        final JSONParser jsonParser = new JSONParser();
        try {
            final JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader("src/main/resources/url.json"));
            final Optional<Object> outerOptionalJsonObject = Optional.ofNullable(jsonObject.get(outerKey));
            if (outerOptionalJsonObject.isPresent()) {
                final JSONObject outerJsonObject = (JSONObject) outerOptionalJsonObject.get();
                final Optional<Object> innerOptionalJsonObject = Optional.ofNullable(outerJsonObject.get(innerKey));
                if (innerOptionalJsonObject.isPresent()) {
                    return Optional.ofNullable(innerOptionalJsonObject.get().toString());
                }
            }
        } catch (final IOException | ParseException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    protected Optional<String> getId(final String name) {
        final JSONParser jsonParser = new JSONParser();
        try {
            final JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader("src/main/resources/privateInfo.json"));
            final Optional<Object> idObject = Optional.ofNullable(jsonObject.get(name));
            if (idObject.isPresent()) {
                return Optional.ofNullable(idObject.get().toString());
            }
        } catch (final IOException | ParseException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
