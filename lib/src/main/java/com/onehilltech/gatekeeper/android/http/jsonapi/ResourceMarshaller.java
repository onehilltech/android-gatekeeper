package com.onehilltech.gatekeeper.android.http.jsonapi;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Map;

public class ResourceMarshaller
    implements JsonDeserializer <Resource>, JsonSerializer <Resource>
{
  private Gson gson_;

  public void setGson (Gson gson)
  {
    this.gson_ = gson;
  }

  @Override
  public Resource deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException
  {
    // The element is a JSON object. Each field in the object should be a registered
    // object. Iterate over each field and convert it to its concrete type.
    JsonObject obj = (JsonObject)json;
    Resource resource = new Resource ();

    for (Map.Entry <String, JsonElement> entry : obj.entrySet ())
    {
      String name = entry.getKey ();
      Type type = Resource.getType (name);

      if (type == null)
        throw new JsonParseException (String.format ("%s type not registered", name));

      Object value = this.gson_.fromJson (entry.getValue (), type);
      resource.add (name, value);
    }

    return resource;
  }

  @Override
  public JsonElement serialize (Resource src, Type typeOfSrc, JsonSerializationContext context)
  {
    JsonObject obj = new JsonObject ();

    for (Map.Entry <String, Object> entry: src.entrySet ())
    {
      String name = entry.getKey ();
      Type type = Resource.getType (name);

      if (type == null)
        throw new JsonParseException (String.format ("%s type not registered", name));

      JsonElement element = this.gson_.toJsonTree (entry.getValue (), type);
      obj.add (name, element);
    }

    return obj;
  }

}
