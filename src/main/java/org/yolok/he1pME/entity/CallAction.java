package org.yolok.he1pME.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@DynamoDBTable(tableName = "CallAction")
public class CallAction {

    @DynamoDBHashKey
    private String action;

    @DynamoDBAttribute(attributeName = "guild_id")
    private String guildId;

    @DynamoDBAttribute
    private String description;

    @DynamoDBAttribute
    private String image;

    @DynamoDBAttribute
    private String message;

    @DynamoDBAttribute(attributeName = "member_names")
    private String memberNames;
}
