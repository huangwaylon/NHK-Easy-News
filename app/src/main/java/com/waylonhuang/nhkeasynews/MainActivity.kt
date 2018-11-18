package com.waylonhuang.nhkeasynews

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, ArticlesFragment.OnFragmentInteractionListener {
    private var navDrawerSelectedIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Setup toolbar.
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Setup drawer.
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.setDrawerListener(toggle)
        toggle.syncState()


        // Setup navigation view.
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Default fragment to select.
        navDrawerSelectedIndex = R.id.nav_home
        if (savedInstanceState != null) {
            navDrawerSelectedIndex = savedInstanceState.getInt(NAV_DRAWER_SELECT_KEY)
        }
        navigationView.setCheckedItem(navDrawerSelectedIndex)
        switchToFragment(navDrawerSelectedIndex)
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            if (navDrawerSelectedIndex != R.id.nav_home) {
                navDrawerSelectedIndex = R.id.nav_home
                switchToFragment(navDrawerSelectedIndex)

                val navigationView = findViewById<NavigationView>(R.id.nav_view)
                navigationView.setCheckedItem(navDrawerSelectedIndex)

                supportActionBar!!.setTitle("NHK Easy News")
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                Toast.makeText(this, "Touch", Toast.LENGTH_SHORT).show()
                onBackPressed()
                return true
            }
            R.id.action_settings -> {
                val articleUrl = "http://www3.nhk.or.jp/news/easy/index.html"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(articleUrl)
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)

        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)

        // Check if we're already at the page.
        //if (id != navDrawerSelectedIndex) {
        navDrawerSelectedIndex = id
        switchToFragment(navDrawerSelectedIndex)
        //}

        return true
    }

    private fun switchToFragment(id: Int) {
        var fragment: Fragment? = null
        var tag: String? = null

        if (id == R.id.nav_home) {
            tag = "home"
            fragment = supportFragmentManager.findFragmentByTag(tag)
            if (fragment == null) {
                fragment = ArticlesFragment.newInstance()
            }
        } else if (id == R.id.nav_settings) {
            tag = "settings"
            fragment = supportFragmentManager.findFragmentByTag(tag)
            if (fragment == null) {
                fragment = SettingsFragment.newInstance()
            }
        }

        if (fragment != null && tag != null) {
            supportFragmentManager.beginTransaction().replace(R.id.flContent, fragment, tag).commit()
        }
    }

    override fun onArticleSelected(articleId: String) {
        val fragment = ArticleDetailFragment.newInstance(articleId)
        supportFragmentManager.beginTransaction()
                .add(R.id.flContent, fragment, articleId)
                .addToBackStack(articleId)
                .commit()

        // Add one so that when in detail, pressing back will allow us to go back.
        navDrawerSelectedIndex += 1
    }

    companion object {
        private val NAV_DRAWER_SELECT_KEY = "NAV_DRAWER_SELECT_KEY"
    }
}
