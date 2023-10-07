package org.yolok.he1pME.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@DynamoDBTable(tableName = "YouTubeNotification")
public class YouTubeNotification {

    @DynamoDBHashKey(attributeName = "youtube_channel_playlist_id")
    private String youtubeChannelPlaylistId;

    @DynamoDBAttribute(attributeName = "message_channel_id")
    private String messageChannelId;

    @DynamoDBAttribute(attributeName = "youtube_channel_name")
    private String youtubeChannelName;
}
