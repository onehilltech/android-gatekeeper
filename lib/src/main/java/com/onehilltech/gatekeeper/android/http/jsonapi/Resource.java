package com.onehilltech.gatekeeper.android.http.jsonapi;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Resource
{
  /// Collection of values in the resource.
  private final HashMap <String, Object> values_ = new HashMap<> ();

  private static final HashMap <String, Type> types_ = new HashMap<> ();

  /**
   * Register a resource type.
   *
   * @param name
   * @param type
   */
  public static void registerType (String name, Type type)
  {
    types_.put (name, type);
  }

  /**
   * Get a registered resource type.
   *
   * @param name
   * @return
   */
  public static Type getType (String name)
  {
    return types_.get (name);
  }

  /**
   * Default constructor.
   */
  public Resource ()
  {

  }

  /**
   * Create a single value resource.
   *
   * @param name
   * @param value
   */
  public Resource (String name, Object value)
  {
    this.values_.put (name, value);
  }

  /**
   * Create a multi-value resource.
   *
   * @param values
   */
  public Resource (Map<String, Object> values)
  {
    this.values_.putAll (values);
  }

  /**
   * Add a new value to the resource.
   *
   * @param name
   * @param value
   */
  public void add (String name, Object value)
  {
    this.values_.put (name, value);
  }

  public Object get (String name)
  {
    return this.values_.get (name);
  }

  @SuppressWarnings ("unchecked")
  public <T> T getAs (String name)
  {
    return (T)this.values_.get (name);
  }

  /**
   * Get the entries in the resource.
   *
   * @return
   */
  public Set<Map.Entry <String, Object>> entrySet ()
  {
    return this.values_.entrySet ();
  }
}
