package org.yolok.he1pME.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.cipher.RemoteCipherManager;
import dev.lavalink.youtube.clients.AndroidMusic;
import dev.lavalink.youtube.clients.AndroidVr;
import dev.lavalink.youtube.clients.ClientOptions;
import dev.lavalink.youtube.clients.MWeb;
import dev.lavalink.youtube.clients.Music;
import dev.lavalink.youtube.clients.Tv;
import dev.lavalink.youtube.clients.TvHtml5Simply;
import dev.lavalink.youtube.clients.Web;
import dev.lavalink.youtube.clients.WebEmbedded;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import org.yolok.he1pME.action.Action;
import org.yolok.he1pME.annotation.He1pME;
import org.yolok.he1pME.entity.BadWord;
import org.yolok.he1pME.repository.BadWordRepository;
import org.yolok.he1pME.repository.TwitchNotificationRepository;
import org.yolok.he1pME.repository.YouTubeNotificationRepository;
import org.yolok.he1pME.service.MusicService;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@EnableDynamoDBRepositories(basePackages = "org/yolok/he1pME/repository")
public class He1pMEConfig {

    @Value("${amazon.aws.accesskey}")
    private String amazonAWSAccessKey;

    @Value("${amazon.aws.secretkey}")
    private String amazonAWSSecretKey;

    @Value("${amazon.aws.region}")
    private String amazonAWSRegion;

    @Value("${youtube.oauth2.refresh-token:}")
    private String youtubeOauth2RefreshToken;

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        AWSCredentials awsCredentials = new BasicAWSCredentials(amazonAWSAccessKey, amazonAWSSecretKey);
        AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
        return AmazonDynamoDBClientBuilder.standard()
                .withCredentials(awsCredentialsProvider)
                .withRegion(Regions.fromName(amazonAWSRegion))
                .build();
    }

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE);
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public Set<GatewayIntent> gatewayIntentSet() {
        return Set.of(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_EXPRESSIONS,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
        );
    }

    @Bean
    public AudioPlayerManager audioPlayerManager(YoutubeAudioSourceManager youtubeAudioSourceManager) {
        DefaultAudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();
        if (StringUtils.isNotBlank(youtubeOauth2RefreshToken)) {
            youtubeAudioSourceManager.useOauth2(youtubeOauth2RefreshToken, true);
        } else {
            youtubeAudioSourceManager.useOauth2(null, false);
        }
        youtubeAudioSourceManager.setCipherManager(new RemoteCipherManager("https://cipher.kikkia.dev/"));
        youtubeAudioSourceManager.getHttpInterfaceManager().configureRequests(config ->
                RequestConfig.copy(config)
                        .setConnectTimeout(10000)
                        .setSocketTimeout(10000)
                        .setConnectionRequestTimeout(10000)
                        .build()
        );
        audioPlayerManager.registerSourceManager(youtubeAudioSourceManager);
        AudioSourceManagers.registerRemoteSources(audioPlayerManager, YoutubeAudioSourceManager.class);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
        return audioPlayerManager;
    }

    @Bean
    public YoutubeAudioSourceManager youtubeAudioSourceManager() {
        ClientOptions fullOptions = new ClientOptions();
        fullOptions.setVideoLoading(true);
        fullOptions.setPlayback(true);
        fullOptions.setSearching(true);
        fullOptions.setPlaylistLoading(true);
        return new YoutubeAudioSourceManager(
                true,
                new Music(fullOptions),
                new Tv(fullOptions),
                new Web(fullOptions),
                new TvHtml5Simply(fullOptions),
                new AndroidMusic(fullOptions),
                new MWeb(fullOptions),
                new WebEmbedded(fullOptions),
                new AndroidVr(fullOptions)
        );
    }

    @Bean
    public Set<String> musicActionSet() {
        return Arrays.stream(MusicService.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.isAnnotationPresent(He1pME.class))
                .map(method -> method.getAnnotation(He1pME.class).instruction())
                .collect(Collectors.toSet());
    }

    @Bean
    public Map<String, Action> customActionMap(List<Action> actions) {
        return actions.stream()
                .filter(action -> action.getClass().isAnnotationPresent(He1pME.class))
                .collect(Collectors.toMap(
                        action -> action.getClass().getAnnotation(He1pME.class).instruction(),
                        Function.identity(),
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }

    @Bean
    public Map<String, Set<String>> badWordMap(BadWordRepository badWordRepository) {
        List<BadWord> badWordList = badWordRepository.findAll();
        if (CollectionUtils.isEmpty(badWordList)) {
            return new ConcurrentHashMap<>();
        }

        return badWordList.stream().collect(Collectors.groupingBy(
                BadWord::getGuildId,
                Collectors.mapping(BadWord::getWord, Collectors.toSet())
        ));
    }

    @Bean
    public Map<String, Set<String>> youtubeNotificationMap(YouTubeNotificationRepository repository) {
        Map<String, Set<String>> map = new ConcurrentHashMap<>();
        repository.findAll().forEach(n ->
                map.computeIfAbsent(n.getYoutubeChannelPlaylistId(), k -> ConcurrentHashMap.newKeySet()).add(n.getMessageChannelId())
        );
        return map;
    }

    @Bean
    public Map<String, Set<String>> twitchNotificationMap(TwitchNotificationRepository repository) {
        Map<String, Set<String>> map = new ConcurrentHashMap<>();
        repository.findAll().forEach(n ->
                map.computeIfAbsent(n.getTwitchChannelId(), k -> ConcurrentHashMap.newKeySet()).add(n.getMessageChannelId())
        );
        return map;
    }
}
