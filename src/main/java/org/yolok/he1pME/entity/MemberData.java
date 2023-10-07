package org.yolok.he1pME.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@DynamoDBTable(tableName = "MemberData")
public class MemberData {

    @DynamoDBHashKey
    private String name;

    @DynamoDBAttribute(attributeName = "guild_id")
    private String guildId;

    @DynamoDBAttribute
    private int blue;

    @DynamoDBAttribute
    private int green;

    @DynamoDBAttribute
    private int red;

    @DynamoDBAttribute(attributeName = "member_id")
    private String memberId;
}
