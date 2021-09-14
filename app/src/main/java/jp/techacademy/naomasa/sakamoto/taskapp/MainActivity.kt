package jp.techacademy.naomasa.sakamoto.taskapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import io.realm.RealmChangeListener
import io.realm.Sort
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import android.app.AlarmManager
import android.app.PendingIntent
import android.util.Log
import androidx.appcompat.widget.SearchView
import io.realm.RealmResults
import kotlinx.android.synthetic.main.content_input.*


class MainActivity : AppCompatActivity() {
    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            // Realmデータベースから、「すべてのデータを取得して新しい日時順に並べた結果」を取得
            val allViewResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
            reloadListView(allViewResults)
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        fab.setOnClickListener {
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this)

        //検索する時
        // Realmデータベースから、「すべてのデータを取得して新しい日時順に並べた結果」を取得
        val allViewResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
//        search.isSubmitButtonEnabled = true

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextChange(newText: String): Boolean {
                // text changed
                val searchViewResults =
                    mRealm.where(Task::class.java).equalTo("category", newText).findAll()
                        .sort("date", Sort.DESCENDING)
                reloadListView(searchViewResults)
                //これがないと検索中、検索文字がないとカテゴリがないものが検索されていることになってしまう
                if (newText == "") {
                    reloadListView(allViewResults)
                }
                return false
            }
            override fun onQueryTextSubmit(query: String): Boolean {
                // submit button pressed
//                val searchViewResults = mRealm.where(Task::class.java).equalTo("category", query).findAll().sort("date", Sort.DESCENDING)
//                reloadListView(searchViewResults)
                return false
            }
        })

        // ListViewをタップしたときの処理
        listView1.setOnItemClickListener { parent, _, position, _ ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this, InputActivity::class.java)
            intent.putExtra(Companion.EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener{ parent, _, position, _ ->
            // タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            val builder = AlertDialog.Builder(this)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK"){_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                //検索中の削除の場合は、検索文字のリセット
                search.setQuery(null, false)
                reloadListView(allViewResults)
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

        reloadListView(allViewResults)
    }

    private fun reloadListView(viewResult: MutableList<Task>) {

        // 上記の結果を、TaskListとしてセットする
        mTaskAdapter.mTaskList = mRealm.copyFromRealm(viewResult)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }

    companion object {
        const val EXTRA_TASK = "jp.techacademy.naomasa.sakamoto.taskapp.TASK"
    }


}