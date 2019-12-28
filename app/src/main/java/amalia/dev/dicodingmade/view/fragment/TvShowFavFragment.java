package amalia.dev.dicodingmade.view.fragment;


import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.material.snackbar.Snackbar;

import amalia.dev.dicodingmade.R;
import amalia.dev.dicodingmade.adapter.TvShowFavAdapter;
import amalia.dev.dicodingmade.adapter.TvShowFavTouchHelper;
import amalia.dev.dicodingmade.model.TvShow;
import amalia.dev.dicodingmade.repository.realm.RealmHelper;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * A simple {@link Fragment} subclass.
 */
public class TvShowFavFragment extends Fragment implements TvShowFavTouchHelper.RecylerItemTouchHelperListener {
    private RecyclerView rv;
    private RealmResults<TvShow> dataLocal;
    private FrameLayout frameLayout;
    private Realm realm;
    private RealmHelper realmHelper;
    private TvShowFavAdapter adapter;
    private RealmChangeListener realmChangeListener;


    public TvShowFavFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tv_show_fav, container, false);
        rv = view.findViewById(R.id.rv_tvshow_fav_fragment);
        frameLayout = view.findViewById(R.id.frameLayout_tv_show_fragment_container);
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));
        rv.setHasFixedSize(true);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.addItemDecoration(new DividerItemDecoration(getActivity(),DividerItemDecoration.VERTICAL));

        //adding item touch listener
        ItemTouchHelper.SimpleCallback listener = new TvShowFavTouchHelper(0,ItemTouchHelper.LEFT,this);
        new ItemTouchHelper(listener).attachToRecyclerView(rv);

        //configuration Realm and load data fav tvshow
        RealmConfiguration configuration = new RealmConfiguration.Builder().build();
        realm = Realm.getInstance(configuration);
        realmHelper = new RealmHelper(realm);
        dataLocal = realmHelper.getListFavoriteTvShows();
        adapter = new TvShowFavAdapter(getActivity(),dataLocal);
        //set adapter into rv
        rv.setAdapter(adapter);

        refresh();

        return  view;
    }
    //when there's change on data, do refresh
    void refresh(){
        realmChangeListener = new RealmChangeListener() {
            @Override
            public void onChange(Object o) {
                adapter = new TvShowFavAdapter(getActivity(),realmHelper.getListFavoriteTvShows());
                rv.setAdapter(adapter);
            }
        };
        realm.addChangeListener(realmChangeListener);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        //pastikan viewholder-nya miliki MovieFavAdapter
        if (viewHolder instanceof TvShowFavAdapter.ViewHolder) {
            //get title movie to show in snacbar when removing
            String name = dataLocal.get(viewHolder.getAdapterPosition()).getOriginalName();
            final TvShow deletedTvShow = dataLocal.get(position);
            //remove favorite movie temporary by set true val tmpDelete
            realmHelper.updateTmpDeleteTS(deletedTvShow.getId(), true);


            //showing snackbar with undo option for restoring deleted movie fav
            Snackbar snackbar = Snackbar.make(frameLayout, name + " deleted from favorites!", Snackbar.LENGTH_SHORT);
            snackbar.setAction("RESTORE", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //restore deleted movie by changing value askedDeletion back to false
                    realmHelper.updateTmpDeleteTS(deletedTvShow.getId(), false);

                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    super.onDismissed(transientBottomBar, event);
                    //when snackbar closed, delete permanently
                    if (deletedTvShow.isTmpDelete()) {
                        realmHelper.deleteFavTvShow(deletedTvShow.getId());
                    }
                }
            });
            snackbar.show();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        realm.close();
    }
}
