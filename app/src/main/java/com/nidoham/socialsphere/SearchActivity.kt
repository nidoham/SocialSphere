package com.nidoham.socialsphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nidoham.socialsphere.ui.screen.SearchScreen
import com.nidoham.socialsphere.ui.theme.SocialSphereTheme

class SearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SocialSphereTheme {
                SearchScreen()
            }
        }
    }
}