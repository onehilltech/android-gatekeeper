package com.raizlabs.android.dbflow.list;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.AsyncTaskLoader;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.runtime.FlowContentObserver;
import com.raizlabs.android.dbflow.sql.queriable.Queriable;
import com.raizlabs.android.dbflow.structure.BaseModelView;
import com.raizlabs.android.dbflow.structure.Model;
import com.raizlabs.android.dbflow.structure.ModelViewAdapter;

/**
 * Utility class to be added to DBFlow.
 *
 * @param <TModel>
 */
@TargetApi(11)
public class FlowModelViewLoader <TModel extends Model, TModelView extends BaseModelView<TModel>>
    extends AsyncTaskLoader<TModelView>
{
  /// Models to be observed for changes.
  private final Class<TModel> mModel;
  private final Class<TModelView> mModelView;

  private final ModelViewAdapter<? extends Model, TModelView> mModelViewAdapter;

  /// Queriable operation that the loader executes.
  private Queriable mQueriable;

  /// Cursor for the loader.
  private TModelView mResult;

  private class ForceLoadContentObserver extends FlowContentObserver
  {
    @Override
    public boolean deliverSelfNotifications ()
    {
      return true;
    }

    @Override
    public void onChange (boolean selfChange)
    {
      super.onChange (selfChange);
      onContentChanged ();
    }

    @Override
    public void onChange (boolean selfChange, Uri uri)
    {
      super.onChange (selfChange, uri);
      onContentChanged ();
    }
  }

  private final ForceLoadContentObserver mObserver = new ForceLoadContentObserver ();

  public FlowModelViewLoader (Context context, Class <TModel> model, Class<TModelView> modelView, Queriable queriable)
  {
    super (context);

    this.mQueriable = queriable;
    this.mModel = model;
    this.mModelView = modelView;
    this.mModelViewAdapter = FlowManager.getModelViewAdapter (modelView);
  }

  /* Runs on a worker thread */
  @Override
  public TModelView loadInBackground ()
  {
    Cursor cursor = this.mQueriable.query ();
    return cursor != null && cursor.moveToFirst () ? this.mModelViewAdapter.loadFromCursor (cursor) : null;
  }

  /* Runs on the UI thread */
  @Override
  public void deliverResult (TModelView result)
  {
    if (this.isReset ())
      return;

    this.mResult = result;

    if (this.isStarted ())
      super.deliverResult (this.mResult);
  }

  /**
   * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
   * will be called on the UI thread. If a previous load has been completed and is still valid the
   * result may be passed to the callbacks immediately.
   * <p>
   * Must be called from the UI thread
   */
  @Override
  protected void onStartLoading ()
  {
    if (mResult != null)
      this.deliverResult (mResult);

    if (this.takeContentChanged () || this.mResult == null)
      this.forceLoad ();

    this.mObserver.registerForContentChanges (this.getContext (), this.mModel);
  }

  /**
   * Must be called from the UI thread
   */
  @Override
  protected void onStopLoading ()
  {
    this.cancelLoad ();
  }

  @Override
  public void onCanceled (TModelView result)
  {
    this.mObserver.unregisterForContentChanges (this.getContext ());
  }

  @Override
  protected void onReset ()
  {
    super.onReset ();

    // Ensure the loader is stopped
    this.onStopLoading ();

    this.mResult = null;
    this.mObserver.unregisterForContentChanges (this.getContext ());
  }

  public Class<TModel> getModel ()
  {
    return this.mModel;
  }
}