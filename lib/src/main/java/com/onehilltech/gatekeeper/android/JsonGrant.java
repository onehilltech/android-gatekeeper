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
    @JsonSubTypes.Type (value=JsonUserCredentials.class, name="password"),
    @JsonSubTypes.Type (value=JsonClientCredentials.class, name="client_credentials"),
    @JsonSubTypes.Type (value=JsonRefreshToken.class, name="refresh_token")})
@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE)
abstract class JsonGrant
{
  @JsonProperty("client_id")
  public String clientId;
}
