package com.raizlabs.android.dbflow.single;

import android.annotation.TargetApi;
import android.content.Context;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.queriable.Queriable;
import com.raizlabs.android.dbflow.structure.BaseQueryModel;
import com.raizlabs.android.dbflow.structure.Model;

import java.util.HashSet;

/**
 * Utility class to be added to DBFlow.
 *
 * @param <TQueryModel>
 */
@TargetApi(11)
public class FlowQueryModelLoader <TQueryModel extends BaseQueryModel>
  extends FlowSingleModelLoader<TQueryModel, TQueryModel>
{
  private HashSet <? extends Model> mModels = new HashSet<> ();

  public FlowQueryModelLoader (Context context,
                               Class <TQueryModel> model,
                               Queriable queriable)
  {
    super (context, model, FlowManager.getQueryModelAdapter (model), queriable);
    this.setObserveModel (false);
  }

  public void registerForContentChanges (Class<? extends Model> model)
  {
    if (!this.mModels.contains (model))
      this.mObserver.registerForContentChanges (this.getContext (), model);
  }
}