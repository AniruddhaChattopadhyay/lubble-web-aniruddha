package in.lubble.app.announcements.announcementHistory;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import in.lubble.app.R;

import static in.lubble.app.utils.FragUtils.replaceFrag;

public class AnnouncementsActivity extends AppCompatActivity {

    public static void newInstance(Context context) {
        final Intent intent = new Intent(context, AnnouncementsActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_announcements);

        Toolbar toolbar = findViewById(R.id.lubble_toolbar);
        setSupportActionBar(toolbar);

        replaceFrag(getSupportFragmentManager(), AnnouncementsFrag.newInstance(), R.id.frame_fragContainer);
    }

}
