package amalia.dev.consumerfavorites.view.tvshow;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Objects;

import amalia.dev.consumerfavorites.R;
import amalia.dev.consumerfavorites.adapter.TvShowAdapter;
import amalia.dev.consumerfavorites.adapter.TvShowFavTouchHelper;
import amalia.dev.consumerfavorites.model.TvShowRealmObject;
import amalia.dev.consumerfavorites.repository.MappingHelper;
import amalia.dev.consumerfavorites.repository.RealmContract;

import static amalia.dev.consumerfavorites.repository.RealmContract.TvShowColumns;

/**
 * A simple {@link Fragment} subclass.
 */
public class TvShowFavFragment extends Fragment implements TvShowFavTouchHelper.RecylerItemTouchHelperListener,LoadTvShowFavCallback {
    private static  final String EXTRA_STATE = "EXTRA_STATE";
    private ConstraintLayout constraintLayout;
    private TextView tvNoFav;
    private ImageView imgNoFav;
    private TvShowAdapter adapter;
    private ProgressBar progressBar;
    private ContentResolver contentResolver;
    private boolean isTmpDeleteFalse;
    private DataObserver dataObserver;


    public TvShowFavFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tv_show_fav, container, false);
        RecyclerView rv = view.findViewById(R.id.recyclerview_tvshowfav);
        progressBar = view.findViewById(R.id.progress_circular_favorites_tvshow);
        constraintLayout = view.findViewById(R.id.constraintLayout_tv_show_fragment_container);
        tvNoFav = view.findViewById(R.id.text_tvshowfav_nofavorites);
        imgNoFav = view.findViewById(R.id.image_tvshowfav_nofavorites);
        adapter = new TvShowAdapter(getActivity());
        contentResolver = Objects.requireNonNull(getActivity()).getContentResolver();

        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));
        rv.setHasFixedSize(true);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.addItemDecoration(new DividerItemDecoration(Objects.requireNonNull(getActivity()),DividerItemDecoration.VERTICAL));

        //adding item touch listener
        ItemTouchHelper.SimpleCallback listener = new TvShowFavTouchHelper(0,ItemTouchHelper.LEFT,this);
        new ItemTouchHelper(listener).attachToRecyclerView(rv);

        //getting data(content provider) in background (async)
        HandlerThread handlerThread = new HandlerThread("DataObserver");
        handlerThread.start();
        Handler handler= new Handler(handlerThread.getLooper());
        //registering an Observer for observing data if there is change
        dataObserver = new DataObserver(handler,getActivity(),this);
        contentResolver.registerContentObserver(RealmContract.TvShowColumns.CONTENT_URI,true,dataObserver);

        //handling data when device is rotating
        if(savedInstanceState == null){
            new LoadTvShowFavAsync(getActivity(),this).execute();
        }else{
            ArrayList<TvShowRealmObject> list = savedInstanceState.getParcelableArrayList(EXTRA_STATE);
            if(list != null){
                adapter.setData(list);
            }
        }

        return  view;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        //pastikan viewholder-nya miliki MovieFavAdapter
        final ContentValues cv = new ContentValues();
        if (viewHolder instanceof TvShowAdapter.ViewHolder) {
            //get title tvshow to show in snackbar when removing
            String name = adapter.getData().get(position).getOriginalName();
            final int idDeletedTvshow = adapter.getData().get(position).getId();
            //remove favorite movie temporary by set true val tmpDelete
            Uri uriUpdate = Uri.parse(TvShowColumns.CONTENT_URI+"/"+idDeletedTvshow);
            cv.put(TvShowColumns.COLUMN_NAME_TMP_DELETE,true);
            final  int rowUpdated = contentResolver.update(uriUpdate,cv,null,null);



            //showing snackbar with undo option for restoring deleted tvshow fav
            if(rowUpdated > 0){
                isTmpDeleteFalse = true;
                Snackbar snackbar = Snackbar.make(constraintLayout, name + " deleted from favorites!", Snackbar.LENGTH_SHORT);
                snackbar.setAction("RESTORE", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //restore deleted movie by changing value askedDeletion back to false
                        Uri uri = Uri.parse(TvShowColumns.CONTENT_URI+"/"+idDeletedTvshow);
                        cv.put(TvShowColumns.COLUMN_NAME_TMP_DELETE,false);
                        contentResolver.update(uri,cv,null,null);
                        isTmpDeleteFalse = false;

                    }
                });
                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        //when snackbar closed, delete permanently
                        if(isTmpDeleteFalse){//because the change will caught by Observer, i use another variable for determining value of tmpDelete is true or false
                            Uri uri = Uri.parse(TvShowColumns.CONTENT_URI+"/"+idDeletedTvshow);
                            contentResolver.delete(uri,null,null);
                        }
                    }
                });
                snackbar.show();
            }

        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(EXTRA_STATE,adapter.getData());
    }

    @Override
    public void preExecute() {
        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void postExecute(ArrayList<TvShowRealmObject> tvshow) {
        progressBar.setVisibility(View.INVISIBLE);
        if(tvshow.size() > 0){
            adapter.setData(tvshow);
        }else{
            adapter.setData(new ArrayList<TvShowRealmObject>());
            tvNoFav.setVisibility(View.VISIBLE);
            imgNoFav.setVisibility(View.VISIBLE);
        }
    }

    static class LoadTvShowFavAsync extends AsyncTask<Void,Void, ArrayList<TvShowRealmObject>> {
        private final WeakReference<Context> weakContext;
        private final WeakReference<LoadTvShowFavCallback> weakCallback;

        LoadTvShowFavAsync(Context context, LoadTvShowFavCallback callback){
            this.weakCallback = new WeakReference<>(callback);
            this.weakContext = new WeakReference<>(context);
        }

        @Override
        protected ArrayList<TvShowRealmObject> doInBackground(Void... voids) {
            Context context = weakContext.get();
            Cursor dataCursor = context.getContentResolver().query(TvShowColumns.CONTENT_URI,null,null,null,null);
            if(dataCursor != null){
                return MappingHelper.tsCursorToArrayList(dataCursor);
            }else {
                return new ArrayList<>();
            }
        }



        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            weakCallback.get().preExecute();
        }

        @Override
        protected void onPostExecute(ArrayList<TvShowRealmObject> tvShowRealmObjects) {
            super.onPostExecute(tvShowRealmObjects);
            weakCallback.get().postExecute(tvShowRealmObjects);
        }
    }

    static  class DataObserver extends ContentObserver{
        final  Context context;
        final LoadTvShowFavCallback callback;

        DataObserver(Handler handler, Context context, LoadTvShowFavCallback callback) {
            super(handler);
            this.callback = callback;
            this.context = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            //when there is change load data (query) in background (async)
            new LoadTvShowFavAsync(context,callback).execute();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        contentResolver.unregisterContentObserver(dataObserver);
    }
}

interface LoadTvShowFavCallback{
    void preExecute();
    void postExecute(ArrayList<TvShowRealmObject> tvshow);
}
