package com.onehilltech.gatekeeper.android;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by hilljh on 11/6/15.
 */
public class RefreshToken extends Grant
{
    @JsonProperty("refresh_token")
    public String refreshToken;
}
