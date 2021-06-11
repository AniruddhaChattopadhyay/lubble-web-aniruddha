package `in`.lubble.app.groups

import `in`.lubble.app.BaseActivity
import `in`.lubble.app.LubbleSharedPrefs
import `in`.lubble.app.R
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar

class ChatGroupListActivity : BaseActivity() {

    companion object {
        public fun open(context: Context) {
            context.startActivity(Intent(context, ChatGroupListActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_group_list)

        val toolbar = findViewById<Toolbar>(R.id.text_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = LubbleSharedPrefs.getInstance().lubbleName + " Chats"

        supportFragmentManager.beginTransaction()
                .replace(R.id.container, GroupsCombinedFrag.newInstance(false))
                .commitNow()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Respond to the action bar's Up/Home button
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}