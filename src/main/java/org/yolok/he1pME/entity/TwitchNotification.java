package org.yolok.he1pME.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@DynamoDBTable(tableName = "TwitchNotification")
public class TwitchNotification {

    @DynamoDBHashKey(attributeName = "twitch_channel_id")
    private String twitchChannelId;

    @DynamoDBAttribute(attributeName = "message_channel_id")
    private String messageChannelId;

    @DynamoDBAttribute(attributeName = "twitch_channel_name")
    private String twitchChannelName;
}
