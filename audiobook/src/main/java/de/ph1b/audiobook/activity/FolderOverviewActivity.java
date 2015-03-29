package de.ph1b.audiobook.activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.FolderOverviewAdapter;
import de.ph1b.audiobook.service.BookAddingService;
import de.ph1b.audiobook.uitools.DividerItemDecoration;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;

public class FolderOverviewActivity extends BaseActivity {

    private static final int REQUEST_NEW_FOLDER = 1;
    private static final String TAG = FolderOverviewActivity.class.getSimpleName();
    private PrefsManager prefs;
    private FolderOverviewAdapter adapter;
    private ArrayList<String> folders;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        L.d(TAG, "onActivityResult, requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
        if (requestCode == REQUEST_NEW_FOLDER && resultCode == Activity.RESULT_OK) {
            String newFolder = data.getStringExtra(FolderChooserActivity.CHOSEN_FOLDER);

            // checking if the folders are a subset of each other
            boolean filesAreSubsets = true;
            boolean firstAddedFolder = folders.size() == 0;
            boolean sameFolder = false;
            for (String s : folders) {
                String[] oldParts = s.split("/");
                String[] newParts = newFolder.split("/");
                for (int i = 0; i < Math.min(oldParts.length, newParts.length); i++) {
                    if (!oldParts[i].equals(newParts[i])) {
                        filesAreSubsets = false;
                    }
                }
                if (s.equals(newFolder)) {
                    sameFolder = true;
                }
                if (!sameFolder && filesAreSubsets) {
                    Toast.makeText(this, getString(R.string.adding_failed_subfolder) + "\n" + s + "\n" + newFolder, Toast.LENGTH_LONG).show();
                }
            }

            if (firstAddedFolder || (!sameFolder && !filesAreSubsets)) {
                adapter.addItem(newFolder);
                prefs.setAudiobookFolders(folders);
                this.startService(BookAddingService.getUpdateIntent(this));
            }
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_overview);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(this.getString(R.string.audiobook_folders_title));

        prefs = new PrefsManager(this);
        folders = prefs.getAudiobookFolders();

        //init views
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        // preparing list
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        adapter = new FolderOverviewAdapter(folders, new FolderOverviewAdapter.OnFolderMoreClickedListener() {
            @Override
            public void onFolderMoreClicked(final int position) {
                new MaterialDialog.Builder(FolderOverviewActivity.this)
                        .title(R.string.delete_folder)
                        .content(getString(R.string.delete_folder_content) + "\n" + adapter.getItem(position))
                        .positiveText(R.string.remove)
                        .negativeText(R.string.dialog_cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                adapter.removeItem(position);
                                prefs.setAudiobookFolders(folders);
                                startService(BookAddingService.getUpdateIntent(FolderOverviewActivity.this));
                            }
                        })
                        .show();
            }
        });
        recyclerView.setAdapter(adapter);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(FolderOverviewActivity.this, FolderChooserActivity.class), REQUEST_NEW_FOLDER);
            }
        });
        fab.attachToRecyclerView(recyclerView);
    }
}
