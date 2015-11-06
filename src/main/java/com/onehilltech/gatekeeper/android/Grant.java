package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use=JsonTypeInfo.Id.NAME,
    include=JsonTypeInfo.As.PROPERTY,
    property="grant_type")
@JsonSubTypes({
    @JsonSubTypes.Type (value=Password.class, name="password"),
    @JsonSubTypes.Type (value=ClientCredentials.class, name="client_credentials"),
    @JsonSubTypes.Type (value=RefreshToken.class, name="refresh_token")})
@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE)
public abstract class Grant
{
    @JsonProperty("client_id")
    public String clientId;
}
